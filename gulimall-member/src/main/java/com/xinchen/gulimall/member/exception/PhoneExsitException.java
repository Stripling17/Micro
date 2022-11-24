package com.xinchen.gulimall.member.exception;

public class PhoneExsitException extends RuntimeException{
    public PhoneExsitException() {
        super("该用户名已存在");
    }

    public PhoneExsitException(String message) {
        super(message);
    }
}
