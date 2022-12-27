package com.xinchen.seckill.controller;

import com.xinchen.common.utils.R;
import com.xinchen.seckill.service.SeckillService;
import com.xinchen.seckill.to.SeckillSKuRedisTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * @return 返回当前时间可以参与秒杀的商品信息
     */
    @ResponseBody
    @GetMapping("/currentSeckillSKus")
    public R getCurrentSeckillSKus() {
        log.info("/currentSeckillSKus正在执行。。。");
        List<SeckillSKuRedisTo> vos = seckillService.getCurrentSeckillSKus();

        return R.ok().setData(vos);
    }

    /**
     * 获取某一个商品的秒杀预告信息
     *
     * @param skuId
     * @return
     */
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SeckillSKuRedisTo to = seckillService.getSkuSeckillInfo(skuId);

        return R.ok().setData(to);
    }

    //http://seckill.gulimall.com/kill?killId=3_1&key=783716318a2649f3b645d21d891fcbf4&num=1
    @GetMapping("/kill")
    public String secKill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model
    ) {
        //1.判断是否登录
        String orderSn = seckillService.kill(killId, key, num);

        model.addAttribute("orderSn",orderSn);

        return "success";
    }


}
