package com.xinchen.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xinchen.common.constant.ProductConstant;
import com.xinchen.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.xinchen.gulimall.product.dao.AttrGroupDao;
import com.xinchen.gulimall.product.dao.CategoryDao;
import com.xinchen.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.xinchen.gulimall.product.entity.AttrGroupEntity;
import com.xinchen.gulimall.product.entity.CategoryEntity;
import com.xinchen.gulimall.product.service.CategoryService;
import com.xinchen.gulimall.product.vo.AttrRespVo;
import com.xinchen.gulimall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.product.dao.AttrDao;
import com.xinchen.gulimall.product.entity.AttrEntity;
import com.xinchen.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();
        //attrEntity.setAttrName(attrVo.getAttrName());
        BeanUtils.copyProperties(attrVo,attrEntity);
        //1.保存基本数据
        this.save(attrEntity);
        //2.如果当前是基础规格属性，需要保存属性分组关联关系
        if(attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
            && attrVo.getAttrGroupId() != null){
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type","base"
                        .equalsIgnoreCase(type)
                        ?ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                        :ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        if(catelogId != 0 ){
            queryWrapper.eq("catelog_id",catelogId);
        }
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            queryWrapper.and((wrapper) ->{
                wrapper.eq("attr_id",key).or().like("attr_name",key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            //如果是基础规格属性才有属性分组，设置属性名称 ===>解决100行：entityOne.getAttrGroupId()空指针异常
            if("base".equalsIgnoreCase(type)){
                //1.设置分类和分组的名字
                AttrAttrgroupRelationEntity entityOne = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                //通过attr_id获取当前属性分组关联表中的属性分组id：若属性分组id为null，空指针异常：attrGroupDao.selectById(attrGroupId).getAttrGroupName()
                if (entityOne != null) {
                    Long attrGroupId = entityOne.getAttrGroupId();
                    if(attrGroupId !=null)
                    attrRespVo.setGroupName(attrGroupDao.selectById(attrGroupId).getAttrGroupName());
                }
            }
            Long catelogId1 = attrEntity.getCatelogId();
            if (!StringUtils.isEmpty(catelogId1)) {
                attrRespVo.setCatelogName(categoryDao.selectById(catelogId1).getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);

        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity,respVo);

        //获取当前修改对象的属性-属性分组关联表
        AttrAttrgroupRelationEntity attrgroupRelation = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
        //获取属性分组ID
        if(!StringUtils.isEmpty(attrgroupRelation) && attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            Long attrGroupId = attrgroupRelation.getAttrGroupId();
            //1.设置属性分组ID
            respVo.setAttrGroupId(attrGroupId);
            //根据当前修改的对象ID拆查询属性分组表；获得当前对象数据
            AttrGroupEntity attrGroupEntity =  attrGroupDao.selectById(attrGroupId);
            //获得属性分组NAME
            if(attrGroupEntity != null){
                String attrGroupName = attrGroupEntity.getAttrGroupName();
                //2.设置属性分组名字
                respVo.setGroupName(attrGroupName);
            }
        }

        //2.获取当前修改对象的分类ID和分类完整路径
        Long catelogId = attrEntity.getCatelogId();
        if(!StringUtils.isEmpty(catelogId)){
            Long[] catelogPath = categoryService.findCatelogPath(catelogId);
            if(!StringUtils.isEmpty(catelogPath)){
                respVo.setCatelogPath(catelogPath);
            }
            //3.获取分类的名称：
            CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
            if(categoryEntity != null){
                respVo.setCatelogName(categoryEntity.getName());
            }
        }
        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        //1.更新属性表
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        this.updateById(attrEntity);

        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                && attr.getAttrGroupId() != null){
            //2.修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());

            //在没有做分组关联前，属性/属性分组关联表是没有当前attr_id关联的分组数据的
            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if(count > 0 ){
                //1.如果当前关系已经存在：修改
                relationDao.update(relationEntity,new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attr.getAttrId()));
            }else {
                //2.否则在关联关系表中新增数据
                relationDao.insert(relationEntity);
            }
        }

    }

    @Transactional
    @Override
    public void removeAttrAndGroupRelation(Collection<? extends Serializable> idList) {
        //1.删除属性分组关联表
        relationDao.deleteBatchByAttrIds(idList);
        //AttrAttrgroupRelationService.remove();

        //2.删除属性表
        this.baseMapper.deleteBatchIds(idList);
    }

    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0){
            return null;
        }

        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;

    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1.当前分组只能关联自己所属的分类中的属性
        //1.1)查询当前分组的分类
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        //2.当前分组只能关联别的分组没有引用的属性
        //2.1)找到当前分类下的其他分组
        List<AttrGroupEntity> otherGroup = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>()
                .eq("catelog_id", catelogId));
        List<Long> otherGroupIds = otherGroup.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());;

        //2.2)这些[其他]分组关联的属性[属性、属性分组关联表中]
        List<AttrAttrgroupRelationEntity> otherAttr = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .in("attr_group_id", otherGroupIds));
        List<Long> attrIds = otherAttr.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        //2.3)从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id",catelogId)
                .eq("attr_type",ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if(attrIds != null && attrIds.size()>0){
            wrapper.notIn("attr_id", attrIds);
        }

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

    @Override
    public void updateCatelogIdByGroupId(Long attrGroupId, Long catelogId) {
        //1 获取当前分组关联的属性
        List<AttrEntity> relationAttr = this.getRelationAttr(attrGroupId);
        //如果当前分组没有关联属性，不执行属性表的更新操作
        if(relationAttr != null){
            //2.运算拿到这些属性的attr_id集合
            List<Long> attrIds = relationAttr.stream().map(item -> {
                Long attrId = item.getAttrId();
                return attrId;
            }).collect(Collectors.toList());
            //3. 更新当前这些属性的分类ID
            this.update(new UpdateWrapper<AttrEntity>().set("catelog_id",catelogId).in("attr_id",attrIds));
        }

    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
        //在指定的所有属性集合里面：挑出检索属性
        // SELECT * FROM pms_attr WHERE attr_id IN(?) AND search_type = 1;
        return this.baseMapper.selectSearchAttrIds(attrIds);

    }


}