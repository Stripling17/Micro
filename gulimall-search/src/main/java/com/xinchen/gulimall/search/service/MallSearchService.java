package com.xinchen.gulimall.search.service;

import com.xinchen.gulimall.search.vo.SearchParam;
import com.xinchen.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param param 检索的所有参数
     * @return 返回检索的结果(里面包含页面的所有信息)
     */
    SearchResult search(SearchParam param);
}
