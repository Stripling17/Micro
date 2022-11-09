package com.xinchen.gulimall.search.service;

import com.xinchen.gulimall.search.vo.SearchParam;

public interface MallSearchService {
    /**
     *
     * @param param 检索的所有参数
     * @return 返回检索的结果
     */
    Object search(SearchParam param);
}
