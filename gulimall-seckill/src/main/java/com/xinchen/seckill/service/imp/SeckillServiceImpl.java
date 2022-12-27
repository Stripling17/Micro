package com.xinchen.seckill.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xinchen.common.to.mq.SeckillOrderTo;
import com.xinchen.common.utils.R;
import com.xinchen.common.vo.MemberRespVo;
import com.xinchen.seckill.feign.CouponFeignService;
import com.xinchen.seckill.feign.ProductFeignService;
import com.xinchen.seckill.interceptor.LoginUserInterceptor;
import com.xinchen.seckill.service.SeckillService;
import com.xinchen.seckill.to.SeckillSKuRedisTo;
import com.xinchen.seckill.vo.SeckillSessionWithSkus;
import com.xinchen.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";  //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.扫描最近三天需要参与秒杀的活动
        R r = couponFeignService.getLatest3DaySession();
        if (r.getCode() == 0) {
            //上架商品
            List<SeckillSessionWithSkus> sessionData = r.getData(new TypeReference<List<SeckillSessionWithSkus>>() {
            });
            //缓存到Redis
            //1.缓存活动信息
            saveSessionInfos(sessionData);
            //2.缓存活动的关联商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

    /**
     * @return 返回当前时间可以参与秒杀的商品信息
     */
    @Override
    public List<SeckillSKuRedisTo> getCurrentSeckillSKus() {
        //1.确定当前时间属于哪一个场次
        long time = new Date().getTime();
        //从redis中获取到所有的场次
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        //seckill:sessions:1671872400000_1671876000000
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long startTime = Long.parseLong(s[0]);
            long endTime = Long.parseLong(s[1]);
            if (time >= startTime && time <= endTime) {
                //此时遍历的场次就是当前秒杀的场次信息
                //2.获取此时场次的商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    List<SeckillSKuRedisTo> collect = list.stream().map(item -> {
                        SeckillSKuRedisTo redisTo = new SeckillSKuRedisTo();
                        SeckillSKuRedisTo redis = JSON.parseObject((String) item, SeckillSKuRedisTo.class);
                        //redis.setRandomCode(null); 当前秒杀开始啦就需要随机码
                        return redis;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }

        //2.获取这个场次所需要的全部商品信息
        return null;
    }

    /**
     * 获取某一个商品的秒杀预告信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSKuRedisTo getSkuSeckillInfo(Long skuId) {
        //1.找到所有需要参与秒杀的key信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                //6_4
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSKuRedisTo redisTo = JSON.parseObject(json, SeckillSKuRedisTo.class);
                    //随机码
                    long currTime = new Date().getTime();
                    if (currTime >= redisTo.getStartTime() && currTime <= redisTo.getEndTime()) {
                    } else {
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }
            }
        }

        return null;
    }

    /**
     * 商品秒杀下单
     *
     * @return 成功后，生成的订单号
     */
    //TODO 上架秒杀商品的时候，每一个数据都有过期时间。
    //TODO 秒杀后续流程，简化了收货地址等信息
    @Override
    public String kill(String killId, String key, Integer num) {
        //long s1 = System.currentTimeMillis();
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();

        //1.获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (StringUtils.hasText(json)) {
            SeckillSKuRedisTo redis = JSON.parseObject(json, SeckillSKuRedisTo.class);
            //1.校验时间合法性
            long currTime = new Date().getTime();
            long ttl = redis.getEndTime() - currTime;
            if (currTime >= redis.getStartTime() && currTime <= redis.getEndTime()) {
                //2.校验随机码和商品id是否正确
                String randomCode = redis.getRandomCode();
                String skuId = redis.getPromotionSessionId().toString() + "_" + redis.getSkuId().toString();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //3.验证购物数量是否合理
                    if (num <= redis.getSeckillLimit()) {
                        //4.验证这个人是否已经购买过。幂等性处理；
                        //如果只要秒杀成功，就去redis占一个位:     userId_sessionId_skuId
                        String redisKey = respVo.getId().toString() + "_" + skuId;
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);//SETNX
                        if (aBoolean) {
                            //占位成功：说明该用户从来没有买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);

                            //boolean b = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功
                                //快速下单。发送MQ消息 10ms
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(orderSn);
                                seckillOrderTo.setMemberId(respVo.getId());
                                seckillOrderTo.setNum(num);
                                seckillOrderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                seckillOrderTo.setSkuId(redis.getSkuId());
                                seckillOrderTo.setSeckillPrice(redis.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrderTo);
                                //long s2 = System.currentTimeMillis();
                                //log.info("耗时...",(s2-s1));
                                return orderSn;
                            }else {
                                //预热的库存不足，此次秒杀失败
                                //秒杀失败：接触用户的抢购限制，便于库存补足后重新秒杀商品
                                redisTemplate.delete(redisKey);
                                return null;
                            }
                        }//else {
                        //说明已经购买过啦
                        //return null;
                        //}
                    }
                }
            }
        }

        return null;
    }

    private void saveSessionInfos(List<SeckillSessionWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            //缓存活动信息
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });

    }

    private void saveSessionSkuInfos(List<SeckillSessionWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                String token = UUID.randomUUID().toString().replace("-", "");

                if (!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString())) {
                    //缓存商品
                    SeckillSKuRedisTo redisTo = new SeckillSKuRedisTo();
                    //1.sku的基本数据
                    R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo info = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }

                    //2.sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);

                    //3.设置上当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //4.随机码？ seckill？skuId=1&randomCode=123dsadfaf12321asdas
                    redisTo.setRandomCode(token);

                    String s = JSON.toJSONString(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), s);

                    //如果当前这个场次的商品库存信息已经上架就不需要上架
                    //引入Redisson客户端的分布式信号量  限流；
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    //5.使用库存作为商品秒杀的信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }
}
