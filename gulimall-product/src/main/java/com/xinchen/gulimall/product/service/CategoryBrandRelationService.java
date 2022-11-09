package com.xinchen.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.product.entity.BrandEntity;
import com.xinchen.gulimall.product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void detailSave(CategoryBrandRelationEntity categoryBrandRelation);

    void updateBrand(Long brandId, String name);

    void updateCategory(Long catId, String name);

    void deleteRelationByBrand(List<Long> BidsList);

    List<BrandEntity> getBrandsByCatId(Long catId);
}

