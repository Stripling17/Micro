package com.xinchen.gulimall.order.web;

import com.xinchen.common.exception.NoStockException;
import com.xinchen.gulimall.order.service.OrderService;
import com.xinchen.gulimall.order.vo.OrderConfirmVo;
import com.xinchen.gulimall.order.vo.OrderSubmitVo;
import com.xinchen.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        //通过HttpServletRequest request可以获取到之前的请求信息
        //然后通过线程共享 ThreadLocal 放入到共享线程当中
        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData", confirmVo);
        //展示订单确认的数据
        return "confirm";
    }

    /**
     * 下单功能
     *
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {
        try {
            //下单：去创建订单，验证令牌，验证价格，锁库存
            SubmitOrderResponseVo resp = orderService.submitOrder(vo);
            //System.out.println("订单提交的数据"+vo);
            if (resp.getCode() == 0) {
                //下单成功，来到支付选择页
                model.addAttribute("submitOrderResp", resp);
                return "pay";
            } else {
                String msg = "下单失败";
                //下单失败回到订单确认页，重新确认订单
                switch (resp.getCode()) {
                    case 1:
                        msg += "订单信息过期：请重新提交";
                        break;
                    case 2:
                        msg += "订单商品发生变化，请确认后再次提交";
                        break;
                    case 3:
                        msg += "库存锁定失败，商品库存不足";
                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e) {
            if (e instanceof NoStockException) {
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }

    }

}
