package com.xinchen.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xinchen.gulimall.product.dao.BrandDao;
import com.xinchen.gulimall.product.dao.CategoryDao;
import com.xinchen.gulimall.product.entity.BrandEntity;
import com.xinchen.gulimall.product.entity.CategoryEntity;
import com.xinchen.gulimall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.product.dao.CategoryBrandRelationDao;
import com.xinchen.gulimall.product.entity.CategoryBrandRelationEntity;
import com.xinchen.gulimall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    BrandDao brandDao;

    @Autowired
    CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void detailSave(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        //1.查询当前品牌的详情
        BrandEntity brandEntity = brandDao.selectById(brandId);
        //2.查询当前分类的详情
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        //3.将查询到的数据放入参数[categoryBrandRelation]【品牌分类关联关系实体】
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        //3.保存到数据库
        this.save(categoryBrandRelation);
        //或者 this.baseMapper.insert(categoryBrandRelation)
    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);
        this.update(relationEntity,new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id",brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId,name);
    }

    @Override
    public void deleteRelationByBrand(List<Long> BidsList) {
        this.baseMapper.deleteBatchByBids(BidsList);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        List<CategoryBrandRelationEntity> entityList = this.baseMapper.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
        List<BrandEntity> collect = entityList.stream().map(item -> {
            Long brandId = item.getBrandId();
            BrandEntity brandEntity = brandDao.selectById(brandId);
            return brandEntity;
        }).collect(Collectors.toList());

        return collect;
    }

}