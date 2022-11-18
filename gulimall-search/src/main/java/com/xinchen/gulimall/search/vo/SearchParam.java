package com.xinchen.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 *
 * 将页面所有可能传递过来的查询条件，都封装成一个对象
 *
 * catalog3Id=225&keyword=小米&sort=saleCount_desc&hasStock=0/1&brandId=1&brandId=2
 * &attrs=1_其他:安卓&attrs=2_5寸:6寸
 */
@Data
public class SearchParam {

    /**
     * 全文检索 skuTitle,catalogTitle
     */
    private String keyword; //页面传递过来的全文匹配关键字

    private Long catalog3Id; //三级分类ID

    /**
     * 排序
     *  按照销量排序 saleCount
     *      sort=saleCount_asc/desc
     *  按照价格排序 skuPrice
     *      sort=skuPrice_asc/desc
     *  按照综合排序（热度评分）hotScore
     *      sort=hotScore_asc/desc
     */
    private String sort; //排序条件

    /**
     * 过滤：hasStock：是否有货、skuPrice：价格区间、brandId：品牌ID、catalogId：分类Id、attrs：属性值
     *      hasStock=0/1
     *      skuPrice=1_500/_500/500_
     *      attrs=1
     */
    private Integer hasStock; //是否只显示有货  默认都是有库存

    private String skuPrice;  //价格区间查询

    private List<Long> brandId;   //按照品牌进行查询，可以多选

    private List<String> attrs; //按照属性进行筛选

    private Integer pageNum = 1; //页码

    private String _queryString; //原生的所有查询条件查询字符串
}
