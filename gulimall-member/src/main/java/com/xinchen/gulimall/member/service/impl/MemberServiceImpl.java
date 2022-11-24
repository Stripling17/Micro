package com.xinchen.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xinchen.common.utils.HttpUtils;
import com.xinchen.gulimall.member.dao.MemberLevelDao;
import com.xinchen.gulimall.member.entity.MemberLevelEntity;
import com.xinchen.gulimall.member.exception.PhoneExsitException;
import com.xinchen.gulimall.member.exception.UsernameExistException;
import com.xinchen.gulimall.member.vo.MemberLogVo;
import com.xinchen.gulimall.member.vo.MemberRegisterVo;
import com.xinchen.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.member.dao.MemberDao;
import com.xinchen.gulimall.member.entity.MemberEntity;
import com.xinchen.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(levelEntity.getId());

        //检查用户名和手机号是否唯一。为了让Controller能感知异常，异常机制
        this.checkUserNameUnique(vo.getUserName());
        this.checkMobilUnique(vo.getPhone());
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());

        //密码要进行加密存储：MD5不能直接用来加密存储
        //盐值加密；MD5 + 随机 $1$+8位字符
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        //其他信息

        //保存用户
        this.baseMapper.insert(memberEntity);
    }

    @Override
    public void checkUserNameUnique(String username) throws UsernameExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public void checkMobilUnique(String phone) throws PhoneExsitException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExsitException();
        }

    }

    //SELECT * FROM ums_member WHERE username = '' OR mobile = ''
    @Override
    public MemberEntity login(MemberLogVo vo) {
        String loginAcct = vo.getLoginAcct();
        String password = vo.getPassword();

        //去数据库查询
        MemberEntity entity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginAcct).or().eq("mobile", loginAcct));
        if (entity != null) {
            //获取到数据库的password字段
            String passwordDb = entity.getPassword();
            //密码匹配
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //明文密码：password ====== 密文密码：passwordDb
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if (matches) {
                return entity;
            } else {
                return null;
            }
        } else {
            //登录失败
            return null;
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //登录注册合并逻辑
        String uid = socialUser.getUid();
        //1.判断当前社交用户是否已经登陆过系统；
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(memberEntity != null){
            //这个用户已经注册：只需更新令牌和过期时间
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccessToken());
            update.setExpiresIn(socialUser.getExpiresIn());

            this.baseMapper.updateById(update);
            memberEntity.setAccessToken(socialUser.getAccessToken());
            memberEntity.setExpiresIn(socialUser.getExpiresIn());
            return memberEntity;
        }else {
            //2.没有查到当前社交用户对应的记录；我们就需要注册一个
            MemberEntity register = new MemberEntity();
            try{
                //3.查询当前社交用户的社交账号信息（昵称，性别等）
                Map<String, String> query = new HashMap<>();
                query.put("access_token",socialUser.getAccessToken());
                query.put("uid",socialUser.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if(response.getStatusLine().getStatusCode() == 200) {
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    //昵称
                    String name = jsonObject.getString("name");
                    //性别
                    String gender = jsonObject.getString("gender");
                    register.setNickname(name);
                    register.setGender("m".equals(gender)?1:0);
                    //.....
                }
            }catch (Exception e){}
            register.setSocialUid(socialUser.getUid());
            register.setAccessToken(socialUser.getAccessToken());
            register.setExpiresIn(socialUser.getExpiresIn());

            this.baseMapper.insert(register);

            return register;
        }
    }

}
