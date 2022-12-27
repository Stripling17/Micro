package com.xinchen.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {
    private Long addrId;     //收货地址的id
    private Integer payType; //支付方式
    //无需提交购买的商品，去购物车在获取一遍
    //优惠、发票

    private String orderToken; //防重令牌
    private BigDecimal payPrice; //与最后订单生成的支付价格对比
    // 如果价格不同；提示用户订单内容有变动，请确认后付款

    private String remark; //订单备注
    //用户相关信息，直接去session中取出登录的用户
}
