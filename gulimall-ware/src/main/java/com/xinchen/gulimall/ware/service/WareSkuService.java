package com.xinchen.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.to.mq.OrderTo;
import com.xinchen.common.to.mq.StockLockedTo;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.ware.entity.WareSkuEntity;
import com.xinchen.gulimall.ware.vo.LockStockResult;
import com.xinchen.gulimall.ware.vo.SkuHasStockVo;
import com.xinchen.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:41:50
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unLockStock(StockLockedTo to);

    void unLockStock(OrderTo orderTo);
}

