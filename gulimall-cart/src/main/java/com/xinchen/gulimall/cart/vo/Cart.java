package com.xinchen.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 需要计算的属性，必须重写他的get方法，保证每次获取属性都会计算
 */
@Data
public class Cart {
    private List<CartItem> items;

    private Integer countNum; //商品数量

    private Integer countType; //商品类型的数量

    private BigDecimal totalAmount; //商品总价

    private BigDecimal reduce = new BigDecimal("0.00"); //减免价格

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if(items!=null && items.size() > 0){
            for (CartItem item : items) {
                count+= item.getCount();
            }
        }
        return count;
    }

    public Integer getCountType() {
        int count = 0;
        if(items!=null && items.size() > 0){
            for (CartItem item : items) {
                count+= 1;
            }
        }
        return count;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        //计算购物项总价
        if(items!=null && items.size() > 0){
            for (CartItem item : items) {
                item.getTotalPrice();
                amount = amount.add(totalAmount);
            }
        }
        //减去优惠的总价
        amount = amount.subtract(getReduce());

        return amount;
    }


    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}