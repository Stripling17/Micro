package com.xinchen.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.product.entity.AttrEntity;
import com.xinchen.gulimall.product.vo.AttrRespVo;
import com.xinchen.gulimall.product.vo.AttrVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attrVo);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    void removeAttrAndGroupRelation(Collection<? extends Serializable> idList);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    void updateCatelogIdByGroupId(Long attrGroupId, Long catelogId);

//    在指定的所有属性集合里面：挑出检索属性
    List<Long> selectSearchAttrIds(List<Long> attrIds);
}

