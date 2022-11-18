package com.xinchen.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;
import com.xinchen.gulimall.product.dao.AttrGroupDao;
import com.xinchen.gulimall.product.entity.AttrEntity;
import com.xinchen.gulimall.product.entity.AttrGroupEntity;
import com.xinchen.gulimall.product.service.AttrGroupService;
import com.xinchen.gulimall.product.service.AttrService;
import com.xinchen.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.xinchen.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        /* SELECT * FROM PMS_ATTR_GROUP WHERE (CATELOG_ID = ? AND (ATTR_GROUPID_ID = KEY OR ATTR_GROUP_NAME LIKE %KEY%) )*/
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.and((obj) ->{
                obj.eq("attr_group_id",key).or().like("attr_group_name",key);
            });
        }
        if(catelogId == 0){
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }else {
            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),wrapper);
            return new PageUtils(page);
        }
    }

    @Transactional
    @Override
    public void updateAttrAndGroup(AttrGroupEntity attrGroup) {
        //1.级联更新：更新当前分组下的属性 -->分类ID -->catelog_id
        Long attrGroupId = attrGroup.getAttrGroupId();
        Long catelogId = attrGroup.getCatelogId();
        attrService.updateCatelogIdByGroupId(attrGroupId,catelogId);

        //2.更新属性分组表信息
        this.updateById(attrGroup);
    }

    /**
     * 根据分类Id查出所有的分组以及这些分组里面的属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //1.查询分组信息
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        //2.查询所有属性
        List<AttrGroupWithAttrsVo> vos = attrGroupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group, attrGroupWithAttrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrGroupWithAttrsVo.getAttrGroupId());
            attrGroupWithAttrsVo.setAttrs(attrs);
            return attrGroupWithAttrsVo;
        }).collect(Collectors.toList());

        return vos;
    }

    /**
     * 联合查询
     *## 当前spu有多少对应的属性分组 groupName/attrName/attrValues
     * SELECT  pav.spu_id,
     * 	ag.attr_group_id,
     * 	ag.attr_group_name,
     * 	aar.attr_id,
     * 	attr.attr_name,
     * 	pav.attr_value
     * FROM
     * pms_attr_group ag
     * LEFT JOIN
     * pms_attr_attrgroup_relation aar
     * ON aar.attr_group_id=ag.attr_group_id
     * LEFT JOIN pms_attr attr
     * ON attr.attr_id = aar.attr_id
     * LEFT JOIN pms_product_attr_value pav
     * ON pav.attr_id = attr.attr_id
     * WHERE ag.catelog_id = 225 AND pav.spu_id = 1
     */
    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long catalogId, Long spuId) {
        /**
         * 分步查询
         */
        //spuId -> pms_product_attr_value:attr_id,attr_name,attr_value[SpuBaseAttrVo]
        //1.查出当前spu对应的所有属性的分组信息以及当前分组下的所有属性对应的值
//        SkuItemVo.SpuItemAttrGroupVo groupAttr = new SkuItemVo.SpuItemAttrGroupVo();
//        List<SkuItemVo.SpuBaseAttrVo> baseAttrVos = new ArrayList<>();
//        SkuItemVo.SpuBaseAttrVo baseAttr = new SkuItemVo.SpuBaseAttrVo();
//        baseAttr.setAttrName("baseAttrName");
//        List<String> attrValues = new ArrayList<>();
//        baseAttr.setAttrValues(attrValues);
//        baseAttrVos.add(baseAttr);
//        groupAttr.setAttrs(baseAttrVos);
        /**
         * 联合查询
         */
        List<SpuItemAttrGroupVo> vos = this.baseMapper.getAttrGroupWithAttrsBySpuId(catalogId,spuId);
        return vos;
    }


}
