package com.xinchen.gulimall.order.vo;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//订单确认页需要用到的数据
@ToString
@NoArgsConstructor
public class OrderConfirmVo {

    // 收货地址，ums_member_receive_address 表
    List<MemberAddressVo> address;

    //所有选中的购物项
    List<OrderItemVo> items;

    //发票记录....

    //优惠券信息....
    Integer integration;

    //是否有库存的map
    @Setter @Getter
    Map<Long,Boolean> stocks;

    //订单总额
    BigDecimal total;

    //应付价格
    BigDecimal payPrice;

    //防重令牌
    @Setter @Getter
    String orderToken;

    public Integer getCount(){
        Integer i = 0;
        if(items != null){
            for (OrderItemVo item : items) {
                i+=item.getCount();
            }
        }
        return i;
    }

    public List<MemberAddressVo> getAddress() {
        return address;
    }

    public void setAddress(List<MemberAddressVo> address) {
        this.address = address;
    }

    public List<OrderItemVo> getItems() {
        return items;
    }

    public void setItems(List<OrderItemVo> items) {
        this.items = items;
    }

    public Integer getIntegration() {
        return integration;
    }

    public void setIntegration(Integer integration) {
        this.integration = integration;
    }

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(items != null){
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    public BigDecimal getPayPrice() {
//        BigDecimal sum = new BigDecimal("0");
//        if(items != null){
//            for (OrderItemVo item : items) {
//                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
//                sum = sum.add(multiply);
//            }
//        }
//        return sum;
        return getTotal();
    }
}
