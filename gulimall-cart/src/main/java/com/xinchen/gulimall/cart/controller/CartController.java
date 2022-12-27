package com.xinchen.gulimall.cart.controller;

import com.xinchen.common.constant.AuthServerConstant;
import com.xinchen.gulimall.cart.feign.ProductFeignService;
import com.xinchen.gulimall.cart.interceptor.CartInterceptor;
import com.xinchen.gulimall.cart.service.CartService;
import com.xinchen.gulimall.cart.vo.Cart;
import com.xinchen.gulimall.cart.vo.CartItem;
import com.xinchen.gulimall.cart.vo.UserInfoTo;
import org.bouncycastle.math.raw.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    //获取当前用户的所有购物项
    @ResponseBody
    @GetMapping("/currentUserCartItems")
    public List<CartItem> getCurrentUserCartItems() {
        List<CartItem> items = cartService.getCurrentUserCartItmes();
        return items;
    }

    //deleteItem?skuId="+deleteId;
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    ///countItem?skuId="+skuId+"&num="+num;
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {
        cartService.changeCountItem(skuId,num);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    //checkItem?skuId="+skuId+"&check="+(check?1:0)
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {
        cartService.checkItem(skuId,check);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 京东商城：浏览器有一个Cookie：user-key:标识用户身份，一个月后过期
     * 如果第一次使用京东的购物车功能，都会给一个临时用户身份；
     * 浏览器以后保存，每次访问都会带上这个cookie；
     *
     * 登录：session里面有
     * 没登录：按照cookie里面带来的user-key来做
     * 第一次：如果没有临时用户。还需要创建一个临时用户。
     * ==========写一个拦截器，判断用户登录的相关信息===========
     *
     *         //1.快速得到用户信息，登录了有userId，没登录有userKey
     *         //Thread-Local-同一个线程共享数据
     *         UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
     *         System.out.println(userInfoTo);
     *         if(userInfoTo == null){
     *             //没登录获取临时购物车数据
     *         }else {
     *             //登录后获取自己的在线购物车
     *         }
     * @return
     */
    @GetMapping({"/","cart.html"})
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);

        return "cartList";
    }

    /**
     * 添加商品到购物车
     *      re.addFlashAttribute();将数据放在session里面可以在页面取出，并且只能取一次
     *      re.addAttribute("skuId",skuId); 将数据放在URL后面
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        CartItem cartItem = cartService.addToCart(skuId,num);
        redirectAttributes.addAttribute("skuId",skuId);
//        model.addAttribute("item",cartItem);

        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * 跳转到成功页
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId ,Model model) {
        //重定向到成功页面，再次查询购物车数据即可
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item",cartItem);
        return "success";
    }
}
