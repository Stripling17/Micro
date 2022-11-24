package com.xinchen.gulimall.member.exception;

public class UsernameExistException extends RuntimeException{
    public UsernameExistException() {
        super("该手机号码已存在");
    }

    public UsernameExistException(String message) {
        super(message);
    }
}
