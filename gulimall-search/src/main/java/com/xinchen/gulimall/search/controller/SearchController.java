package com.xinchen.gulimall.search.controller;

import com.xinchen.gulimall.search.service.MallSearchService;
import com.xinchen.gulimall.search.vo.SearchParam;
import com.xinchen.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 自动将页面提交过来的所有请求查询参数封装成指定的对象
     * @param param 检索的所有参数
     * @return 返回检索的结果(里面包含页面的所有信息)
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        //将查询字符串放入param参数
        param.set_queryString(request.getQueryString());

        //根据我们页面传递来的查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result",result);


        return "list";
    }
}
