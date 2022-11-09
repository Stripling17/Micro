package com.xinchen.gulimall.product.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xinchen.gulimall.product.entity.CategoryEntity;
import com.xinchen.gulimall.product.service.CategoryService;
import com.xinchen.common.utils.R;



/**
 * 商品三级分类
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 查出所有分类以及子分类，以树形结构组装起来
     */
    @RequestMapping("/list/tree")
    public R list(){
        //返回所有的分类列表 自定义：withTree()-->并以树形结构存储
        List<CategoryEntity> entities = categoryService.listWithTree();
        return R.ok().put("data", entities);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    public R info(@PathVariable("catId") Long catId){
		CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("data", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryEntity category){
		categoryService.save(category);

        return R.ok();
    }

    /**
     * 实现三级分类的拖拽功能
     * 批量修改分类的信息
     */
    @RequestMapping("/update/sort")
    public R updateSort(@RequestBody List<CategoryEntity> category){
        categoryService.updateBatchById(category);

        return R.ok();
    }
    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryEntity category){
		categoryService.updateCascade(category);

        return R.ok();
    }

    /**
     * 删除
     * @RequestBody：想要获取请求体，必须发送POST请求
     * SpringMVC自动将请求体中的数据（json），转为对象
     */
    @RequestMapping("/delete")
    //将json对象转为Long类型的数组
    public R delete(@RequestBody Long[] catIds){
        //1.检查当前删除的菜单，是否被别的地方引用 【字段关联】
		//categoryService.removeByIds(Arrays.asList(catIds));

        categoryService.removeMenuByIds(Arrays.asList(catIds));

        return R.ok();
    }

}
