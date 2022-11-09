package com.xinchen.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinchen.gulimall.product.entity.BrandEntity;
import com.xinchen.gulimall.product.vo.BrandVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xinchen.gulimall.product.entity.CategoryBrandRelationEntity;
import com.xinchen.gulimall.product.service.CategoryBrandRelationService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 获取当前品牌管理的所有分类列表
     */
//    @RequestMapping(value = "/catelog/list", method = RequestMethod.GET)
    @GetMapping("/catelog/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R catelogList(@RequestParam Long brandId){
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>()
                .eq("brand_id",brandId)
        );

        return R.ok().put("data", data);
    }

    /**
     * /product/categorybrandrelation/brands/list
     *
     * 1.Controller:处理请求，接受和校验数据
     * 2.Service接受Controller传来的数据，进行业务处理
     * 3.Controller接受Service处理完的数据，封装页面指定的Vo
     */


    @GetMapping("/brands/list")
    public R relationBrandsList(@RequestParam(value = "catId" , required = true) Long catId){
        List<BrandEntity> brandEntities =  categoryBrandRelationService.getBrandsByCatId(catId);
        List<BrandVo> brandVoList = brandEntities.stream().map(entity -> {
            BrandVo brandVo = new BrandVo();
            brandVo.setBrandId(entity.getBrandId());
            brandVo.setBrandName(entity.getName());
            return brandVo;
        }).collect(Collectors.toList());

        return R.ok().put("data",brandVoList);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.detailSave(categoryBrandRelation);
        //TODO 更新其他关联
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
