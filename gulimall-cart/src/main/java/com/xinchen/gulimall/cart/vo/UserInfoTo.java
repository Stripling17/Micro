package com.xinchen.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;     //一定会封装一个临时用户

    private boolean tempUser = false;
}
