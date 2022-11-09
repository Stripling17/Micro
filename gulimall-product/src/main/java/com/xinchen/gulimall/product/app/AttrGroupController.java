package com.xinchen.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.xinchen.gulimall.product.entity.AttrEntity;
import com.xinchen.gulimall.product.service.AttrAttrgroupRelationService;
import com.xinchen.gulimall.product.service.AttrService;
import com.xinchen.gulimall.product.service.CategoryService;
import com.xinchen.gulimall.product.vo.AttrGroupRelationVo;
import com.xinchen.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xinchen.gulimall.product.entity.AttrGroupEntity;
import com.xinchen.gulimall.product.service.AttrGroupService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.R;



/**
 * 属性分组
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrAttrgroupRelationService relationService;

//    /product/attrgroup/attr/relation
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> relationList){
        relationService.saveBatch(relationList);
        return R.ok();
    }

//    /product/attrgroup/{catelogId}/withattr
    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catelogId") Long catelogId){
        //1.查出当前分类下的所有属性分组
        //2.查出每个属性分组的所有属性
        List<AttrGroupWithAttrsVo> vos =  attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        return  R.ok().put("data",vos);
    }

//    /product/attrgroup/{attrgroupId}/attr/relation

    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId){
        List<AttrEntity> entities = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data",entities);
    }

//    /product/attrgroup/{attrgroupId}/noattr/relation
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R noAttrRelation(@PathVariable("attrgroupId") Long attrgroupId ,
                            @RequestParam Map<String, Object> params){
        PageUtils page = attrService.getNoRelationAttr(params,attrgroupId);
        return R.ok().put("page",page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params , @PathVariable("catelogId") Long catelogId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params,catelogId);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        Long[] catelogPath =  categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(catelogPath);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateAttrAndGroup(attrGroup);

        return R.ok();
    }

//    /product/attrgroup/attr/relation/delete
    @RequestMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody List<AttrGroupRelationVo> attrGroupRelationVo){
        relationService.deleteRelation(attrGroupRelationVo);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
