package com.xinchen.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.product.entity.BrandEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
public interface BrandService extends IService<BrandEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void updateDetail(BrandEntity brand);

    void removeBrandByIds(List<Long> BidsList);
}

