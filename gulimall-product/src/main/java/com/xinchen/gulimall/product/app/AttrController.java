package com.xinchen.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.xinchen.gulimall.product.entity.ProductAttrValueEntity;
import com.xinchen.gulimall.product.service.ProductAttrValueService;
import com.xinchen.gulimall.product.vo.AttrRespVo;
import com.xinchen.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xinchen.gulimall.product.entity.AttrEntity;
import com.xinchen.gulimall.product.service.AttrService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.R;



/**
 * 商品属性
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

//    /product/attr/base/listforspu/{spuId}
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrListForSpu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> entities = productAttrValueService.baseAttrListForSpu(spuId);

        return R.ok().put("data",entities);
    }


//    /product/attr/base/list/{catelogId}
//    /product/attr/sale/list/0?
    @GetMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String type){
        PageUtils page = attrService.queryBaseAttrPage(params,catelogId,type);
        return R.ok().put("page",page);
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     * /product/attrgroup/info/{attrGroupId}
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
		AttrEntity attr = attrService.getById(attrId);
		AttrRespVo respVo =  attrService.getAttrInfo(attrId);

        return R.ok().put("attr", respVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attrVo){
		attrService.saveAttr(attrVo);

        return R.ok();
    }

    /**
     * 修改商品规格
     */
//    /product/attr/update/{spuId}
    @PostMapping("/update/{spuId}")
    public R updateSpuAttrForSpuId(@PathVariable("spuId") Long spuId,
                                    @RequestBody List<ProductAttrValueEntity> entities) {
        productAttrValueService.updateSpuAttrForSpuId(spuId,entities);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds){
		//attrService.removeByIds(Arrays.asList(attrIds));
		attrService.removeAttrAndGroupRelation(Arrays.asList(attrIds));
        return R.ok();
    }


}
