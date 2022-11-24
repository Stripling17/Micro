package com.xinchen.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.xinchen.common.constant.AuthServerConstant;
import com.xinchen.common.exception.BizCodeEnume;
import com.xinchen.common.utils.R;
import com.xinchen.common.vo.MemberRespVo;
import com.xinchen.gulimall.auth.feign.MemberFeignService;
import com.xinchen.gulimall.auth.feign.ThirdPartFeignService;
import com.xinchen.gulimall.auth.vo.UserLogVo;
import com.xinchen.gulimall.auth.vo.UserRegisterVo;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送一个请求直接跳转到一个页面
     * SpringMVC viewController:将请求和页面映射过来
     */
//    @GetMapping({"/","/login.html"})
//    public String toLogin(){
//        return "login";
//    }

    /**
     * 发送验证码验证
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 接口防刷。

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.hasText(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                //60s内不能重复发送验证码给用户
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2.验证码的再次校验 =》 Redis =>存key:phone value:code
        String code = UUID.randomUUID().toString().substring(0, 4) + "_" + System.currentTimeMillis();
        //redis缓存验证码：防止同一个手机号在60s内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(phone, code.split("_")[0]);
        return R.ok();
    }


    /**
     *
     *  //TODO 重定向携带数据，利用session原理。将数据放在session中
     *    只要跳转到下一个页面取出这个数据后，session里面的数据就会被删除
     *
     *  //TODO 1、分布式下的session问题
     * 会员注册
     * @param RedirectAttributes redirectAttributes:模拟重定向携带数据
     * @return 注册成功跳转
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            /**
             *.map(fieldError -> {
             *                 String field = fieldError.getField();
             *                 String errorMessage = fieldError.getDefaultMessage();
             *                 errors.put(field,errorMessage);
             *             })
             */
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
//            model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            //Request method 'POST' not supported
            // 用户注册 -> register[post] -> 转发register.html ===路径映射默认都是get方式访问的
            //校验出错，转发到注册页
//            return "forward:/register.html";  //return "register"视图解析器拼串
            return "redirect:http://auth.gulimall.com/register.html";
        }
        //1.校验验证码是否正确
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(StringUtils.hasText(s)){
            //真正注册：调用远程服务进行注册
            if(code.equals(s.split("_")[0])){
                //删除缓存中的验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //调用远程服务进行注册
                R r = memberFeignService.register(vo);
                if(r.getCode() == 0){
                    //成功
                    return "redirect:/login.html";
                }else {
                    Map<String,String > errors = new HashMap<>();
                    errors.put("msg",r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/login.html";
                }

            //验证码不通过
            }else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/register.html";
            }
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }

        //注册成功回到首页(登录页面)
//        return "redirect:/login.html";
//        return "redirect:http://auth.gulimall.com/login.html";
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute != null) {
            //已登录
            return "redirect:http://gulimall.com/";
        }else {
//            未登录
            return "login";
        }
    }

    @PostMapping("/login")
    public String login(UserLogVo vo, RedirectAttributes redirectAttributes,
                        HttpSession session) {
        //远程登录
        R r = memberFeignService.login(vo);
        if(r.getCode() == 0){
            //成功
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com/";
        }else {
            Map<String,String > errors = new HashMap<>();
            errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
            //失败：重新定位到登录页面
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

}
