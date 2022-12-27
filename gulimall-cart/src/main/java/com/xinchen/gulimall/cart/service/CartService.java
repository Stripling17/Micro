package com.xinchen.gulimall.cart.service;

import com.xinchen.gulimall.cart.vo.Cart;
import com.xinchen.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {

    /**
     * 将商品添加到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车中某个购物项
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    //清除临时购物车的数据
    void clearCart(String cartKey);

    //勾选购物项
    void checkItem(Long skuId, Integer check);

    //修改购物项数量
    void changeCountItem(Long skuId, Integer num);

    //根据skuId删除一个购物项
    void deleteItem(Long skuId);

    //获取当前用户的所有购物项
    List<CartItem> getCurrentUserCartItmes();
}
