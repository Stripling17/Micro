package com.xinchen.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.product.entity.CategoryEntity;
import com.xinchen.gulimall.product.vo.Catelog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查出所有分类以及子分类，以树形结构组装起来
     */
    List<CategoryEntity> listWithTree();

    /**
     * 批量删除商品分类信息
     * @param asList
     */
    void removeMenuByIds(List<Long> asList);

    //找到catelogId完整路径
    //【父/子/孙】
    Long[] findCatelogPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    /**
     * 获取一级分类
     * @return
     */
    List<CategoryEntity> getLevel1Categorys();

    Map<String, List<Catelog2Vo>> getCatelogJson();

}

