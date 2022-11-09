package com.xinchen.gulimall.product.service.impl;

import com.xinchen.gulimall.product.vo.AttrGroupRelationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.xinchen.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.xinchen.gulimall.product.service.AttrAttrgroupRelationService;


@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                new Query<AttrAttrgroupRelationEntity>().getPage(params),
                new QueryWrapper<AttrAttrgroupRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void deleteRelation(List<AttrGroupRelationVo> attrGroupRelationVo) {
//        this.baseMapper.delete(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",1L).eq("attr_group_id",1L));

//        List<AttrAttrgroupRelationEntity> entities = attrGroupRelationVo.stream().map((item) -> {
//            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
//            BeanUtils.copyProperties(item, relationEntity);
//            return relationEntity;
//        }).collect(Collectors.toList());
//        this.baseMapper.deleteBatchRelation(entities);

        this.baseMapper.deleteBatchRelation(attrGroupRelationVo);
    }

    /**
     * 批量保存
     * @param relationList
     */
    @Override
    public void saveBatch(List<AttrGroupRelationVo> relationList) {
        List<AttrAttrgroupRelationEntity> entities = relationList.stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        this.saveBatch(entities);
    }

}