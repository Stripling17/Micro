package com.xinchen.gulimall.order.interceptor;

import com.xinchen.common.constant.AuthServerConstant;
import com.xinchen.common.vo.MemberRespVo;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //"/order/order/status/234322324321231")


        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", uri);
        boolean match1 = antPathMatcher.match("/payed/notify", uri);
        if (match || match1) {
            return true;
        }

        MemberRespVo memberRespVo = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);

        if(memberRespVo != null){

            loginUser.set(memberRespVo);
            return true;
        }else {
            //没登录就去登录
            request.getSession().setAttribute("msg","订单登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
