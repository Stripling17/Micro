package com.xinchen.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.to.SkuReductionTo;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 10:16:46
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void SkuReduction(SkuReductionTo skuReductionTo);
}

