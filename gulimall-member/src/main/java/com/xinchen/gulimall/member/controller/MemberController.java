package com.xinchen.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.xinchen.common.exception.BizCodeEnume;
import com.xinchen.gulimall.member.exception.PhoneExsitException;
import com.xinchen.gulimall.member.exception.UsernameExistException;
import com.xinchen.gulimall.member.feign.CouponFeignService;
import com.xinchen.gulimall.member.vo.MemberLogVo;
import com.xinchen.gulimall.member.vo.MemberRegisterVo;
import com.xinchen.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xinchen.gulimall.member.entity.MemberEntity;
import com.xinchen.gulimall.member.service.MemberService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.R;

import javax.annotation.Resource;


/**
 * 会员
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:31:04
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R memberCoupons = couponFeignService.memberCoupons();

        return R.ok().put("member",memberEntity).put("coupons",memberCoupons.get("coupons"));
    }

    @PostMapping("/oauth2/login")
    public R oauth2Login(@RequestBody SocialUser socialUser) throws Exception {

        MemberEntity entity = memberService.login(socialUser);
        if(entity != null){
            return R.ok().setData(entity);
        }else {
            return R.error(BizCodeEnume.LOGINACCT_PASSWORD_INVALILD_EXCEPITON.getCode(),BizCodeEnume.LOGINACCT_PASSWORD_INVALILD_EXCEPITON.getMsg());
        }
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLogVo vo) {

        MemberEntity entity = memberService.login(vo);
        if(entity != null){
            return R.ok().setData(entity);
        }else {
            return R.error(BizCodeEnume.LOGINACCT_PASSWORD_INVALILD_EXCEPITON.getCode(),BizCodeEnume.LOGINACCT_PASSWORD_INVALILD_EXCEPITON.getMsg());
        }
    }

    @PostMapping("/register")
    //远程调用传对象会转为Json
    public R register(@RequestBody MemberRegisterVo vo) {
        try {
            memberService.register(vo);
        }catch (UsernameExistException u){
            return R.error(BizCodeEnume.USER_EXIST_EXCEPITON.getCode(), BizCodeEnume.USER_EXIST_EXCEPITON.getMsg());
        }catch (PhoneExsitException p) {
            return R.error(BizCodeEnume.PHONE_EXIST_EXCEPITON.getCode(), BizCodeEnume.PHONE_EXIST_EXCEPITON.getMsg());
        }
        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
