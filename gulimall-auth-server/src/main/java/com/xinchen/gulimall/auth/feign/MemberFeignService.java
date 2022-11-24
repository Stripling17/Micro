package com.xinchen.gulimall.auth.feign;

import com.xinchen.common.utils.R;
import com.xinchen.gulimall.auth.vo.SocialUser;
import com.xinchen.gulimall.auth.vo.UserLogVo;
import com.xinchen.gulimall.auth.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/register")
    //远程调用传对象会转为Json
    R register(@RequestBody UserRegisterVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLogVo vo);

    /**
     * 社交用户注册或登录
     * @param socialUser
     * @return
     * @throws Exception
     */
    @PostMapping("/member/member/oauth2/login")
    R oauth2Login(@RequestBody SocialUser socialUser) throws Exception;
}
