package com.xinchen.gulimall.product.dao;

import com.xinchen.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchen.gulimall.product.vo.AttrGroupRelationVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    void deleteBatchByAttrIds(@Param("Coll") Collection<? extends Serializable> idList);

    void deleteBatchRelation(@Param("Coll") List<AttrGroupRelationVo> attrGroupRelationVo);
}
