package com.xinchen.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareRespVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
