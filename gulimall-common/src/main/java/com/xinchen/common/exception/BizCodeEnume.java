package com.xinchen.common.exception;

/**
 * 错误码和错误信息定义类
 * 1.错误码定义规则为5位数字
 * 2.前两位表示使用的业务场景，最后三位表示错误码，例如：10001   通用：10  系统未知异常：001
 * 3.维护错误码需要维护错误描述，将他们定义位枚举形式
 * 错误码列表：
 * 10：通用
 *      001:参数格式校验失败
 *      002:短信验证码频率太高
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14：物流
 * 15:用户
 */
public enum BizCodeEnume {
    UNKNOW_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10001,"参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002,"验证码获取频率太高，请稍后再试"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPITON(15001,"用户存在异常"),
    PHONE_EXIST_EXCEPITON(15002,"手机号存在异常"),
    LOGINACCT_PASSWORD_INVALILD_EXCEPITON(15003,"账号密码错误");

    private Integer code;
    private String msg;

    BizCodeEnume(Integer code , String msg){
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
