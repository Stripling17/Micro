package com.xinchen.gulimall.ware.feign;

import com.xinchen.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-gateway")
public interface ProductFeignService {

    /**
     * 信息
     *  1.直接请求对应服务
     *  /product/skuinfo/info/{skuId}
     *
     *  2.让所有请求都过一下网关
     *  1）@FeignClient("gulimall-gateway")
     *  2）/api/product/skuinfo/info/{skuId}
     */
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

}
