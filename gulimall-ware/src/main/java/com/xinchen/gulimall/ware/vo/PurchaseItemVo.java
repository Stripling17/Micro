package com.xinchen.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PurchaseItemVo {
//    itemId:1,status:4,reason:""

    @NotNull
    private Long itemId; //采购项ID

    private Integer status; //采购项状态

    private String reason; //采购失败的原因
}
