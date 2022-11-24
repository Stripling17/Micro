package com.xinchen.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.member.entity.MemberEntity;
import com.xinchen.gulimall.member.exception.PhoneExsitException;
import com.xinchen.gulimall.member.exception.UsernameExistException;
import com.xinchen.gulimall.member.vo.MemberLogVo;
import com.xinchen.gulimall.member.vo.MemberRegisterVo;
import com.xinchen.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:31:04
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVo vo);

    void checkUserNameUnique(String username) throws UsernameExistException;

    void checkMobilUnique(String phone) throws PhoneExsitException;

    MemberEntity login(MemberLogVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

