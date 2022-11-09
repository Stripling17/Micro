package com.xinchen.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrRespVo extends AttrVo{

    //所属分类名字
    private String catelogName;

    //所属分组名字
    private String groupName;

    //分组路径[2,3,225]
    private Long[] catelogPath;
}
