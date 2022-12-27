package com.xinchen.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.exception.NoStockException;
import com.xinchen.common.to.mq.OrderTo;
import com.xinchen.common.to.mq.SeckillOrderTo;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;
import com.xinchen.common.utils.R;
import com.xinchen.common.vo.MemberRespVo;
import com.xinchen.gulimall.order.constant.OrderConstant;
import com.xinchen.gulimall.order.dao.OrderDao;
import com.xinchen.gulimall.order.entity.OrderEntity;
import com.xinchen.gulimall.order.entity.OrderItemEntity;
import com.xinchen.gulimall.order.entity.PaymentInfoEntity;
import com.xinchen.gulimall.order.enume.OrderStatusEnum;
import com.xinchen.gulimall.order.feign.CartFeignService;
import com.xinchen.gulimall.order.feign.MemberFeignService;
import com.xinchen.gulimall.order.feign.ProductFeignService;
import com.xinchen.gulimall.order.feign.WmsFeignService;
import com.xinchen.gulimall.order.interceptor.LoginUserInterceptor;
import com.xinchen.gulimall.order.service.OrderItemService;
import com.xinchen.gulimall.order.service.OrderService;
import com.xinchen.gulimall.order.service.PaymentInfoService;
import com.xinchen.gulimall.order.to.OrderCreateTo;
import com.xinchen.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> threadLocal = new ThreadLocal<>();

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
//        System.out.println("主线程..."+Thread.currentThread().getId());
        //requestAttributes包括当前请求和请求中的所有信息
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //远程查询所有的收货地址列表
//            System.out.println("member线程..."+Thread.currentThread().getId());
            //将requestAttributes重新放回当前异步线程的RequestContextHolder中
//            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addressList = memberFeignService.getAddress(memberRespVo.getId());
            orderConfirmVo.setAddress(addressList);
        }, executor);

        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            //远程查询购物车所有选中的购物项
//            System.out.println("cart线程..."+Thread.currentThread().getId());
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(currentUserCartItems);
            //feign在远程调用之前，要构造请求。会调用很多的拦截器
            //RequestInterceptor interceptor : requestInterceptors
        }, executor).thenRunAsync(()-> {
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R r = wmsFeignService.getSkusHasStock(collect);
            if(r.getCode() == 0){
                List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
                });
                if(data != null){
                    Map<Long, Boolean> collect1 = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                    orderConfirmVo.setStocks(collect1);
                }
            }

        },executor);


        //3.查询用户积分:在我们ThreadLocal中用户session转化为的MemberRespVo对象中包含
        orderConfirmVo.setIntegration(memberRespVo.getIntegration());

        //4.其他数据自动计算

        //TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-" , "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespVo.getId().toString(),token,30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFuture,getCartFuture).get();


        return orderConfirmVo;
    }

    //同一个对象内事务方法互调默认失效，原因，绕过了代理对象
    //事务使用代理对象来控制的
    @Transactional(timeout = 30)
    public void a() { //a事务：a事务的所有设置就传播到了和他共用一个事务的方法；
        //b c 方法做任何设置都没有用。都是和a共用一个事务
        OrderServiceImpl orderService = (OrderServiceImpl) AopContext.currentProxy();
        orderService.b();
        orderService.c();
//        b();    //a事务：需要一个事务，但是如果当前方法已经有事务存在，就与当前调用方法共用一个事务
//        c();    //new事务（不回滚）
        int i = 10/0;
    }

    //如果当前方法有被其他方法调用，且调用方法是一个事务，就与调用方法共用一个事务
    @Transactional(propagation = Propagation.REQUIRED) //默认传播方式就是(REQUIRED , timeout = 30)
    public void b() {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void c() {}

    /**
     * 下订单方法实现
     */
    //本地事务，在分布式系统下，只能控制住自己的回滚，控制不了其他事务的回滚
    //分布式事务：最大原因。网络问题+分布式机器。
    //读未提交：脏读
    //mysql默认的隔离级别：(isolation = Isolation.REPEATABLE_READ) 可重复读；
    //Oracle和SqlServer的默认个隔离级别 isolation = Isolation.READ_COMMITTED 读已提交
    //序列化（串行化）：【SERIALIZABLE】
    //@GlobalTransactional 高并发
    @Transactional
    //(propagation = Propagation.REQUIRES_NEW)
    //传播行为：propagation = Propagation.REQUIRED/Propagation.REQUIRES_NEW
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        SubmitOrderResponseVo respVo = new SubmitOrderResponseVo();

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        respVo.setCode(0);
        threadLocal.set(submitVo);
        //1.验证令牌【令牌的对比和删除必须保证原子性】
        String key = OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId().toString();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = submitVo.getOrderToken();
        //String redisToken = redisTemplate.opsForValue().get(key);
        //原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class), Arrays.asList(key),orderToken);
        if(result == 0L){
            //令牌验证失败
            respVo.setCode(1);
            return respVo;
        }else {
            //令牌验证成功
            //下单：去创建订单，验证令牌，验证价格，锁库存
            //创建订单、订单项信息
            OrderCreateTo order = createOrder();
            //2.验证价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01){
                //金额对比成功
                //TODO 3.保存订单
                saveOrder(order);
                //TODO 4.库存锁定，只要有异常，回滚订单数据
                //订单号，所有订单项（skuId，skuName，num）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());

                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //为了保证高并发。库存服务自己回滚。可以发消息给库存服务；
                //库存服务本身也可以使用自动解锁模式==使用消息队列来完成自动自动解锁
                R r = wmsFeignService.orderLockStock(lockVo);
                //库存成功啦，但是网络原因超时，订单回滚，库存不滚
                if(r.getCode() == 0) {
                    //锁定成功
                    respVo.setOrder(order.getOrder());
                    //TODO 5.远程扣减积分
//                    int i = 10/0;
                    //TODO 订单创建成功发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());
                    return respVo;
                }else {
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
//                    respVo.setCode(3);
//                    return respVo;
                }
            }else {
                //金额对比失败
                respVo.setCode(2);
                return respVo;
            }
        }
//        if(orderToken != null && orderToken.equals(redisToken)){
//            //令牌验证通过，删除令牌
//            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId().toString())
//
//        }else {
//
//        }
    }

    /**
     * 返回订单状态
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        //关闭订单前：查询当前订单最新状态
        OrderEntity orderEntity = this.baseMapper.selectById(entity.getId());
        //待付款状态下超时：
        if(orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            //过了三十分钟收到的消息，可能内容出现变化，如果直接拿对象属性更新可能出现更新错误
            OrderEntity update = new OrderEntity();
            //关单
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.baseMapper.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            try{
                //TODO 保证消息一定会发送出去，每一个消息都可以做好日志记录（给数据库保存每一个消息的详细信息）
                //TODO 定期扫描数据库；将失败的消息在发送一遍
                rabbitTemplate.convertAndSend("order-event-exchange","order.release.other.#",orderTo);
            }catch (Exception e) {
                //TODO 将没发送成功的消息进行重试操作
            }
        }
    }

    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);
        if(order != null){
            //订单号
            payVo.setOut_trade_no(order.getOrderSn());

            //订单的主题（名称） / 备注
            List<OrderItemEntity> orderItems = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
            OrderItemEntity item = orderItems.get(0);
            String skuName = item.getSkuName();
            payVo.setSubject(skuName);
            payVo.setBody(item.getSkuAttrsVals());

            //订单总额：保留两位小数，自动向上进步；
            BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
            payVo.setTotal_amount(bigDecimal.toString());

        }
        return payVo;
    }

    /**
     * 分页查询当前登录用户的所有订单信息
     * 带有订单详情数据的列表
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo member = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",member.getId()).orderByDesc("id")
        );
        List<OrderEntity> orderEntities = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(orderEntities);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝的支付结果
     * @param vo
     * @return
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //1.保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(infoEntity);

        //2.修改订单的状态信息
        if(vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED") ) {
            //支付成功状态
            String outTradeNo = vo.getOut_trade_no();
            //this.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
            this.baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    /**
     * 秒杀单信息创建
     * @param seckillOrder
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());

        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);

        //TODO 保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrder.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        //TODO 获取当前SKU的详细信息进行设置productFeignService.getSpuInfoBySkuId()
        orderItemEntity.setSkuQuantity(seckillOrder.getNum());

        orderItemService.save(orderItemEntity);

    }

    /**
     * 保存订单的所有数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setCreateTime(new Date());
//        this.baseMapper.insert(orderEntity);
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();

        //1.生成一个订单号 Mybatis
        String orderSn = IdWorker.getTimeId();
        //2.获取收货地址信息与运费金额信息 ==》OrderSubmitVo
        OrderEntity orderEntity = buildOrder(orderSn);
        //3.获取到所有的购物项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        //4.计算价格/积分等相关信息
        computePrice(orderEntity,orderItemEntities);
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);


        return createTo;
    }

    /**
     * 计算价格和积分
     * @param orderEntity
     * @param itemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        //订单价格相关
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        BigDecimal total = new BigDecimal("0.0");

        BigDecimal giftIntegration = new BigDecimal("0.0");
        BigDecimal giftGrowth = new BigDecimal("0.0");


        //订单总额:叠加每一个订单项的总额数据
        for (OrderItemEntity item : itemEntities) {
            //各种优惠总价
            BigDecimal realAmount = item.getRealAmount();
            coupon = coupon.add(item.getCouponAmount());
            integration = integration.add(item.getIntegrationAmount());
            promotion =promotion.add(item.getPromotionAmount());

            //订单总价
            total = total.add(realAmount);

            //积分信息
            giftIntegration = giftIntegration.add(new BigDecimal(item.getGiftIntegration().toString()));
            //成长值信息
            giftGrowth = giftGrowth.add(new BigDecimal(item.getGiftGrowth().toString()));
        }
        //1.订单总价
        orderEntity.setTotalAmount(total);
        //2.应付总价
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        //封装各种优惠总价
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);

        //设置积分等信息
        orderEntity.setIntegration(giftIntegration.intValue());
        orderEntity.setGrowth(giftGrowth.intValue());
        //设置未删除
        orderEntity.setDeleteStatus(0);
    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        //构建会员的ID：member_id
        orderEntity.setMemberId(respVo.getId());

        OrderSubmitVo submitVo = threadLocal.get();
        R r = wmsFeignService.getFare(submitVo.getAddrId());
        if(r.getCode() == 0){
            FareVo fareResp = r.getData(new TypeReference<FareVo>() {
            });
            //设置运费金额
            orderEntity.setFreightAmount(fareResp.getFare());
            //设置收货人信息
            orderEntity.setReceiverProvince(fareResp.getAddress().getProvince());
            orderEntity.setReceiverCity(fareResp.getAddress().getCity());
            orderEntity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
            orderEntity.setReceiverPostCode(fareResp.getAddress().getPostCode());
            orderEntity.setReceiverName(fareResp.getAddress().getName());
            orderEntity.setReceiverPhone(fareResp.getAddress().getPhone());
            orderEntity.setReceiverRegion(fareResp.getAddress().getRegion());
        }

        //设置订单的相关状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        return orderEntity;
    }

    /**
     * 构建全部订单项列表
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems != null && currentUserCartItems.size() > 0){
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);

                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建某一个订单项中的数据
     * @param item
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity itemEntity = new OrderItemEntity();

        //1、商品的spu信息
        Long skuId = item.getSkuId();
        //获取spu的信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfoData.getId());
        itemEntity.setSpuName(spuInfoData.getSpuName());
        itemEntity.setSpuBrand(spuInfoData.getBrandName());
        itemEntity.setCategoryId(spuInfoData.getCatalogId());

        //2、商品的sku信息
        itemEntity.setSkuId(skuId);
        itemEntity.setSkuName(item.getTitle());
        itemEntity.setSkuPic(item.getImage());
        itemEntity.setSkuPrice(item.getPrice());
        itemEntity.setSkuQuantity(item.getCount());

        //使用StringUtils.collectionToDelimitedString将list集合转换为String
        String skuAttrValues = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttrValues);

        //3、商品的优惠信息

        //4、商品的积分信息
        itemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
        itemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

        //5、订单项的价格信息
        itemEntity.setPromotionAmount(BigDecimal.ZERO);
        itemEntity.setCouponAmount(BigDecimal.ZERO);
        itemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //当前订单项的实际金额.总额 - 各种优惠价格
        //原来的价格
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        //原价减去优惠价得到最终的价格
        BigDecimal subtract = origin.subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);

        return itemEntity;

    }

}
