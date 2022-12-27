package com.xinchen.gulimallmember;

import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@SpringBootTest
class GulimallMemberApplicationTests {

    @Test
    void contextLoads() {
        //抗修改值：彩虹表
//        DigestUtils.md5DigestAsHex("")
        //盐值加密：随机值
        //$1$/HcSxqIE$CmlE04A9GIMyrtrWxbryV.
        String s = Md5Crypt.md5Crypt("123456".getBytes(StandardCharsets.UTF_8));
        System.out.println(s);
        //如何验证，从数据库中获取盐值后在对当前用户输入的密码进行加密，比对当前加密后的结果和数据库中已存储的加密结果是否相同

        //Spring提供的密码编码器:通过加密后的密码可以自动检测出当前密码加密的盐值
        //$2a$10$h8CtgasPDYDCrdCSAFWfzO5avC0hIkX5QjyCXCZV4jJxMvGfbljhm
        //$2a$10$1FsE1Nkq6zJv0WLxdVytc.pr9732wvWWSEGz0UJ.mOyMg1xNm8XEy
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        System.out.println(encode);

        boolean matches = passwordEncoder.matches("123456", "$2a$10$1FsE1Nkq6zJv0WLxdVytc.pr9732wvWWSEGz0UJ.mOyMg1xNm8XEy");
        System.out.println(matches);
    }

}
