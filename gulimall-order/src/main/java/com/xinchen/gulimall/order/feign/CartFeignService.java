package com.xinchen.gulimall.order.feign;

import com.xinchen.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    //获取当前用户的所有购物项
    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
