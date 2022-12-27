package com.xinchen.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xinchen.common.utils.R;
import com.xinchen.gulimall.cart.feign.ProductFeignService;
import com.xinchen.gulimall.cart.interceptor.CartInterceptor;
import com.xinchen.gulimall.cart.service.CartService;
import com.xinchen.gulimall.cart.vo.Cart;
import com.xinchen.gulimall.cart.vo.CartItem;
import com.xinchen.gulimall.cart.vo.SkuInfoVo;
import com.xinchen.gulimall.cart.vo.UserInfoTo;
import io.netty.util.concurrent.CompleteFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //1.获取用户信息，向redis中添加当前用户的购物车信息
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String item = (String) cartOps.get(skuId.toString());
        if (!StringUtils.hasText(item)) {
            //购物车无此商品
            CartItem cartItem = new CartItem();
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //2.远程查询当前要添加的商品信息
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                    });
                    //3.添加新商品到购物车
                    cartItem.setSkuId(skuId);
                    cartItem.setImage(skuInfo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfo.getSkuTitle());
                    cartItem.setCheck(true);
                    cartItem.setCount(num);
                    cartItem.setPrice(skuInfo.getPrice());
                }
            }, executor);

            CompletableFuture<Void> getSkuAttrTask = CompletableFuture.runAsync(() -> {
                //3.远程查询sku的组合信息
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                if (values != null && values.size() > 0) {
                    cartItem.setSkuAttr(values);
                }
            }, executor);

            CompletableFuture.allOf(getSkuInfoTask, getSkuAttrTask).get();

            String jsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), jsonString);
            return cartItem;
        } else {
            //购物车有此商品，修改购物车的数量即可
            CartItem cartItem = JSON.parseObject(item, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);

            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    /**
     * 获取购物车中指定商品的购物项
     *
     * @param skuId
     * @return
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            //登录后获取自己的在线购物车
            String cartId = CART_PREFIX + userInfoTo.getUserId();
            //2.如果临时购物车的数据还没有进行合并
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null) {
                //临时购物车有数据，需要合并
                for (CartItem tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
                //清除临时购物车的数据
                clearCart(tempCartKey);
            }
            //获取登录后的购物车的数据 【包含合并来的临时购物车的数据，和登录后的用户购物车数据】
            List<CartItem> cartItems = getCartItems(cartId);
            cart.setItems(cartItems);

        } else {
            //没登录获取临时购物车数据
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        }
        return cart;
    }

    /**
     * 获取到我们要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //1.获取用户信息，向redis中添加当前用户的购物车信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //gulimall:cart:1
            cartKey = CART_PREFIX + userInfoTo.getUserId().toString();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

//        redisTemplate.opsForHash().get(cartKey, {1,"{skuId:1}"});
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }

    /**
     * 获取临时购物车的所有购物项
     * 也可以获得用户购物车的所有项
     */
    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            List<CartItem> collect = values.stream().map((obj) -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    //清除临时购物车的数据
    public void clearCart(String cartKey) {
//        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
//        hashOps.delete()
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1 ? true : false);
        String jsonItem = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), jsonItem);
    }

    @Override
    public void changeCountItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getCurrentUserCartItmes() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartId = CART_PREFIX + userInfoTo.getUserId().toString();
            //获取当前用户的所有购物项
            List<CartItem> cartItems = getCartItems(cartId);
            //获取当前用户所有被选中的购物项并更新当前购物项的信息
            List<CartItem> checkedItems = cartItems.stream().filter(item ->
                    item.getCheck()).map((item) -> {
                //更新为最新价格
                BigDecimal price = productFeignService.getPrice(item.getSkuId());
                if (price != null) {
                    item.setPrice(price);
                }
                return item;
            }).collect(Collectors.toList());
            return checkedItems;
        }
    }
}
