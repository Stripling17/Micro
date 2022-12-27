package com.xinchen.gulimall.member.interceptor;

import com.xinchen.common.constant.AuthServerConstant;
import com.xinchen.common.vo.MemberRespVo;
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

        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String uri = request.getRequestURI();
        if(antPathMatcher.match("/member/member/**", uri)) {
            return true;
        }

        if(antPathMatcher.match("/member/memberreceiveaddress/**",uri)){
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
