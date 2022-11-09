package com.xinchen.gulimall.search.controller;

import com.xinchen.gulimall.search.service.MallSearchService;
import com.xinchen.gulimall.search.vo.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam param){
        Object result = mallSearchService.search(param);
        return "list";
    }
}
