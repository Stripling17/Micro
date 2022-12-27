package com.xinchen.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.xinchen.common.exception.NoStockException;
import com.xinchen.common.to.mq.OrderTo;
import com.xinchen.common.to.mq.StockDetailTo;
import com.xinchen.common.to.mq.StockLockedTo;
import com.xinchen.common.utils.R;
import com.xinchen.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.xinchen.gulimall.ware.entity.WareOrderTaskEntity;
import com.xinchen.gulimall.ware.feign.OrderFeignService;
import com.xinchen.gulimall.ware.feign.ProductFeignService;
import com.xinchen.gulimall.ware.service.WareOrderTaskDetailService;
import com.xinchen.gulimall.ware.service.WareOrderTaskService;
import com.xinchen.gulimall.ware.vo.OrderItemVo;
import com.xinchen.gulimall.ware.vo.OrderVo;
import com.xinchen.gulimall.ware.vo.SkuHasStockVo;
import com.xinchen.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.ware.dao.WareSkuDao;
import com.xinchen.gulimall.ware.entity.WareSkuEntity;
import com.xinchen.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService orderTaskService;

    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    /**
     * 库存自动解锁：
     * if(detail != null) {
     * //通过详情的任务单号查询当前订单是否存在
     * WareOrderTaskEntity task = orderTaskService.getById(detail.getTaskId());
     * //存在
     * if(detail != null){
     * <p>
     * }else {
     * if(订单 == 已付款) {
     * //从锁定库存==》实际减少库存
     * }
     * if(订单 == 超时自动取消) {
     * //解锁库存；
     * }
     * }
     * }else {
     * /没有详情任务单：锁库存回滚，任务单信息回滚
     * }
     *
     * @param message
     */

    /**
     * 解锁库存
     */
    private void unLockedStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //库存解锁
        this.baseMapper.unLockedStock(skuId, wareId, num);
        //更新库存工作单
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2); //1.已锁定 2.已解锁
        orderTaskDetailService.updateById(entity);
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo hasStockVo = new SkuHasStockVo();
            //1.查询当前sku的总库存量
            //SELECT SUM(stock-stock_locked) FROM wms_ware_sku WHERE sku_id = 10
            Long count = this.baseMapper.getSkuStock(skuId);

            hasStockVo.setSkuId(skuId);
            hasStockVo.setHasStock(count == null ? false : count > 0);

            return hasStockVo;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        /**
         * wareId: 1
         * skuId: 2
         */
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1.判断如果还没有这个库存记录，则新增
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //远程查询sku的名字
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
                //如果失败；整个事务不需要回滚
            } catch (Exception e) {
                //1.自己catch掉异常

            }
            this.baseMapper.insert(wareSkuEntity);
        } else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }

    }

    /**
     * 为某个订单锁定库存
     * (rollbackFor = NoStockException.class)
     * 默认只要是运行时异常：都会回滚
     *
     * @param vo 库存解锁的场景
     *           1）下订单成功，订单过期没有支付被系统自动取消、被用户手动取消，都要解锁库存
     *           2）下订单成功、库存也成功，但是接下来的业务调用失败，导致订单回滚。
     *           之前锁定的库存在一段时间以后就要自动解锁
     *           <p>
     *           3）只要解锁库存的消息失败，一定要告诉服务器，此次解锁是失败的
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单的详情，作用追溯，便于回滚
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);

        //按照下单的收货地址，找到一个就近仓库，锁定库存
        //我们就不这么复杂，挨个找仓库锁库存，锁成功就用谁的

        //1.找到每个商品在那个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> hasStockWare = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪里有库存
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //锁定库存
        for (SkuWareHasStock hasStock : hasStockWare) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                throw new NoStockException(skuId);
            } else {
                //1.如果每一个商品都锁定成功，将当前商品锁定了几件的工作单记录发送给MQ
                //2.锁定失败。前面保存的工作单就回滚啦。发送出去的消息也没有问题，即使要解锁记录。
                //  由于查不到指定id，就找不到具体锁库存数量，所以不用解锁
                for (Long wareId : wareIds) {
                    //成功就返回1 ，否则就是0
                    Long count = this.baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                    if (count == 1) {
                        skuStocked = true;
                        //TODO 告诉MQ库存锁定成功
                        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                        orderTaskDetailService.save(taskDetailEntity);
                        StockLockedTo stockLockedTo = new StockLockedTo();
                        stockLockedTo.setId(taskEntity.getId());

                        StockDetailTo stockDetailTo = new StockDetailTo();
                        BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                        //只发详情单ID不行，防止回滚以后找不到数据
                        stockLockedTo.setDetail(stockDetailTo);
                        rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                        break;
                    } else {
                        //当前仓库锁失败，重试下一个仓库
                    }
                }
                if (skuStocked == false) {
                    //当前商品所有仓库都没有锁住
                    throw new NoStockException(skuId);
                }
            }
        }

        //如果没有异常抛出：说明每个商品都锁成功啦
        return true;
    }

    @Override
    public void unLockStock(StockLockedTo to) {
        Long taskId = to.getId();//库存工作单的id
        StockDetailTo detailTo = to.getDetail();
        Long detailId = detailTo.getId();
        //解锁：查询数据库关于这个订单的锁定库存信息(详细信息是否还存在)
        WareOrderTaskDetailEntity taskDetailEntity = orderTaskDetailService.getById(detailId);
        if (taskDetailEntity != null) {
            //获取订单信息
            WareOrderTaskEntity task = orderTaskService.getById(taskId);
            //根据订单号查询订单的状态
            R r = orderFeignService.getOrderStatus(task.getOrderSn());
            if (r.getCode() == 0) {
                OrderVo order = r.getData(new TypeReference<OrderVo>() {
                });
                if (order == null || order.getStatus() == 4) {
                    //订单不存在
                    //订单已经被取消啦。才能解锁库存
                    if (taskDetailEntity.getLockStatus() == 1) {
                        //当前库存工作单详情，状态是1 ：已锁定但是未解锁 才可以解锁
                        unLockedStock(detailTo.getSkuId(), detailTo.getWareId(), detailTo.getSkuNum(), detailId);
                    }
                }
            } else {
                //消息拒绝以后重新放倒队列里面，让别人继续消费解锁
                throw new RuntimeException("远程服务失败");
            }
        } else {
            // 若没有：无需解锁
            // 库存锁定就失败啦，库存自动回滚，任务详情单自动回滚==》数据库无信息
        }
    }

    /**
     * 方式订单服务卡顿，导致订单状态消息一直改不了，库存消息悠闲到期，差订单状态肯定是新建状态，什么都不做就走啦
     * 导致当前卡顿的订单，永远都无法解锁库存
     *
     * @param orderTo
     */
    @Override
    public void unLockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下最新的状态
        //R r = orderFeignService.getOrderStatus(orderSn);

        //查一下最新的的库存解锁状态，方式重复解锁库存
        WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        //按照工作单找到所有 没有解锁的库存，进行解锁
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unLockedStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }

    }
}


@Data
class SkuWareHasStock {
    private Long skuId;
    private Integer num;
    private List<Long> wareId;
}


