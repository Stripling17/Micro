package com.xinchen.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.xinchen.common.exception.BizCodeEnume;
import com.xinchen.common.exception.NoStockException;
import com.xinchen.gulimall.ware.vo.SkuHasStockVo;
import com.xinchen.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xinchen.gulimall.ware.entity.WareSkuEntity;
import com.xinchen.gulimall.ware.service.WareSkuService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.R;



/**
 * 商品库存
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:41:50
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo){

        try {
            Boolean lockStock =wareSkuService.orderLockStock(vo);
            return R.ok().setData(lockStock);
        }catch (NoStockException e) {
            System.out.println(e.getMessage());
            return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(),BizCodeEnume.NO_STOCK_EXCEPTION.getMsg());
        }

    }

    /**
     * 查询sku是否有库存
     */
    @PostMapping("/hasStock")
    public R getSkusHasStock(@RequestBody List<Long> skuIds){

        //sku_id  stock
        List<SkuHasStockVo> stockVos = wareSkuService.getSkusHasStock(skuIds);
        /**
         *          因为R<T>是一个HashMap的原因，自己扩展的私有属性全部失效
         *         //R<List<SkuHasStockVo>> ok = R.ok();
         *         //ok.setData(stockVos);
         *         //return ok;
         */
        return R.ok().setData(stockVos);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
