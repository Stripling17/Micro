package com.xinchen.gulimall.product.feign;

import com.xinchen.common.utils.R;
import com.xinchen.gulimall.product.feign.fallback.SeckillFeignServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "gulimall-seckill" , fallback = SeckillFeignServiceFallBack.class)
public interface SeckillFeignService {
    /**
     * 获取某一个商品的秒杀预告信息
     * @param skuId
     * @return
     */
    @GetMapping("/sku/seckill/{skuId}")
    R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
