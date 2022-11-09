package com.xinchen.gulimall.product.feign;

import com.xinchen.common.to.SkuHasStockVo;
import com.xinchen.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

//Feign客户端
@FeignClient("gulimall-ware")
public interface WareFeignService {
    /**
     * 关于返回值的调用
     * 1.R设计的时候可以加上泛型
     * 2.直接返回我们想要的结果
     * 3.自己封装解析结果
     * 查询sku是否有库存
     */
    @PostMapping("/ware/waresku/hasStock")
    R getSkusHasStock(@RequestBody List<Long> skuIds);
}
