package com.xinchen.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xinchen.common.constant.AuthServerConstant;
import com.xinchen.common.utils.HttpUtils;
import com.xinchen.common.utils.R;
import com.xinchen.gulimall.auth.feign.MemberFeignService;
import com.xinchen.common.vo.MemberRespVo;
import com.xinchen.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class Oauth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 社交登录成功回调
     * @param code
     * @return
     * @throws Exception
     */
    @GetMapping("/oauth/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session, HttpServletResponse servletResponse) throws Exception {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "3620300326");
        map.put("client_secret", "6cc1942acb5e6ae51ef61b2cb119e21e");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth/weibo/success");
        map.put("code", code);
        //1.根据code换取accessToken；
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", headers, querys, map);

        //处理access_token
        if (response.getStatusLine().getStatusCode() == 200){
            //获取响应状态码；为200则成功
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //通过socialUser就知道登录的是哪个社交用户
            //1)、当前用户如果是第一次进网站，自动注册进来（为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定的会员）
            //登录或者注册这个社交用户
            R r = memberFeignService.oauth2Login(socialUser);
            if(r.getCode() == 0){
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登录成功：用户信息{}",data.toString());
                //1.第一次使用session：命令浏览器保存cookie令牌：cookie：JSESSIONID
                //以后浏览器访问哪个网站就会带上这个网站的cookie
                //子域之间：[父域名]gulimall.com [子]auth.gulimall.com search.gulimall.com order.gulimall.com
                //在发卡的时候（指定域名为父域名），即使是子域系统发的卡，也能让父域直接使用
                //TODO 1.默认发的令牌。session=jsessionfdsa。作用域：当前域：（解决子域session共享问题）
                //TODO 2.使用JSON的序列化方式来序列化对象数据到redis中
                //2.登录成功，调回首页
                session.setAttribute(AuthServerConstant.LOGIN_USER,data);
//                new Cookie("JSESSIONID","sdfasdf").setDomain();
//                servletResponse.addCookie();
                return "redirect:http://gulimall.com/";
            }else {
                return "redirect:http://auth.gulimall.com/login.html";
            }

        }else {
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

}
