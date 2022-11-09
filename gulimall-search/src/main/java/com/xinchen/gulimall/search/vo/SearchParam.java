package com.xinchen.gulimall.search.vo;

import lombok.Data;

/**
 * 封装页面所有可能传递过来的查询条件
 */
@Data
public class SearchParam {

    private String keyword; //页面传递过来的全文匹配关键字

    private Long catalog3Id; //三级分类ID
}
