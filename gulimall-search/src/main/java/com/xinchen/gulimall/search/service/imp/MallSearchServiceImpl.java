package com.xinchen.gulimall.search.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xinchen.common.to.es.SkuEsModel;
import com.xinchen.common.utils.R;
import com.xinchen.gulimall.search.config.GulimallElasticSearchConfig;
import com.xinchen.gulimall.search.constant.EsConstant;
import com.xinchen.gulimall.search.feign.ProductFeignService;
import com.xinchen.gulimall.search.service.MallSearchService;
import com.xinchen.gulimall.search.vo.AttrResponseVo;
import com.xinchen.gulimall.search.vo.BrandVo;
import com.xinchen.gulimall.search.vo.SearchParam;
import com.xinchen.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.bouncycastle.util.encoders.UTF8;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;


    /**
     * @param param 检索的所有参数
     * @return 返回检索的结果(里面包含页面的所有信息)
     */
    @Override
    public SearchResult search(SearchParam param) {
        //动态构建出查询需要的DSL语句
        SearchResult result = null;
        //1.准备检索请求  buildSearchRequest：构建检索的方法
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);//COMMON_OPTIONS：设置项操作

            //3.分析响应数据封装成我们需要的格式
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    /**
     * 构建检索请求:
     * #模糊匹配，过滤(按照价格区间，分类，品牌，属性，库存)，排序，分页，高亮，聚合分析
     *
     * @return 返回按查询条件构建好的ElasticSearch检索请求
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        //SearchSourceBuilder构建DSL语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /**
         * 查询条件：过滤(按照价格区间，分类，品牌，属性，库存)
         */
        //1）通过QueryBuilders工具类搬我们构建出bool -- Query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //2）must - 模糊匹配
        if (StringUtils.hasText(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        //3）filter - 过滤
        // (1)按照价格区间来进行检索
        if (StringUtils.hasText(param.getSkuPrice())) {
            //price;1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                //区间
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(s[0]);
                }

                if (param.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(s[0]);
                }
            }

            boolQuery.filter(rangeQuery);
        }

        // (2)按照三级分类id查询
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        // (3)按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }

        // (4)按照所哟指定的属性进行查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                // attrs=1_其他:安卓&attrs=2_5寸:6寸
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                //attr = 1_其他:安卓
                String[] s = attrStr.split("_");
                String attrId = s[0]; //检索的属性id
                String[] attrValues = s[1].split(":"); //这个属性id所具有的属性值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));

                // 每一个都得必须生成一个nested（嵌入式）查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // (5)按照是否有库存来进行查询
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        //把以前所有的条件都拿来封装
        sourceBuilder.query(boolQuery);

        /**
         * 排序，分页，高亮
         */

        // 4）排序
        if (StringUtils.hasText(param.getSort())) {
            String sort = param.getSort();
            //sort=saleCount_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }

        // 5）分页
        // pageNum:1 from:0 size:5 [0,1,2,3,4]
        // pageNum:2 from:5 size:5 [5,6,7,8,9]
        //from = (pageNum - 1 ) * PageSize
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 6）高亮：只有在用skuTitle做模糊匹配【全文检索】的时候才使用
        if (StringUtils.hasText(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle").preTags("<b style=color:red'>").postTags("</b>");

            sourceBuilder.highlighter(builder);
        }

        /**
         * 聚合
         */
        //TODO（1）品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg")
                .field("brandId").size(10);
        // 品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg")
                .field("brandName").size(1));

        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg")
                .field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        //TODO（2）分类聚合 catalog_agg
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg")
                .field("catalogId").size(10);
        // 分类聚合的子聚合
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg")
                .field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        //TODO（3）属性聚合 nested：内嵌
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合出当前所有attrId ==》attr_id_agg
        TermsAggregationBuilder attr_id_agg = AggregationBuilders
                .terms("attr_id_agg")
                .field("attrs.attrId").size(10);
        //【子聚合】分析出当前attr_id对应的所有名字和属性值
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg")
                .field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg")
                .field("attrs.attrValue").size(10));

        attr_agg.subAggregation(attr_id_agg);
        //将整理好的内嵌的聚合放入sourceBuilder用来检索
        sourceBuilder.aggregation(attr_agg);

        String s = sourceBuilder.toString();
        System.out.println("构建的DSL" + s);

        // 创建一个检索请求，指定构造参数
        // 索引：String[]{EsConstant.PRODUCT_INDEX} 构建器：SearchSourceBuilder sourceBuilder
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }

    /**
     * 构建检索结果
     *
     * @return 对检索结果的封装
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();
        //1.返回的所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                if (StringUtils.hasText(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLightText = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(highLightText);
                }
                esModels.add(esModel);
            }
        }

        result.setProducts(esModels);

        //2.返回的查询到的商品集合属于那些分类
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();

        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");

        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo vo = new SearchResult.CatalogVo();
            //得到分类ID
            String catalogId = bucket.getKeyAsString();
            vo.setCatalogId(Long.parseLong(catalogId));

            //得到分类名
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            vo.setCatalogName(catalogName);

            catalogVos.add(vo);
        }
        result.setCatalogs(catalogVos);

        //3.返回的查询到的商品集合包含的品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();

        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");

        List<? extends Terms.Bucket> buckets1 = brand_agg.getBuckets();
        for (Terms.Bucket bucket : buckets1) {
            SearchResult.BrandVo vo = new SearchResult.BrandVo();
            //得到品牌的ID
            long brandId = bucket.getKeyAsNumber().longValue();
            vo.setBrandId(brandId);

            //得到品牌的名字
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            vo.setBrandName(brandName);

            //得到品牌的图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            vo.setBrandImg(brandImg);

            brandVos.add(vo);
        }
        result.setBrands(brandVos);

        //4.返回的查询到的商品设计到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //获得属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            //获得属性的名字
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            //获得属性的值
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValue = attr_value_agg.getBuckets().stream().map(item -> {
                String value = item.getKeyAsString();
                return value;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValue);

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);
        //================以上从聚合信息获取====================

        //4.分页信息->当前页码
        result.setPageNum(param.getPageNum());

        //5.分页信息->总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        //6.分页信息->总页数(页码) ==计算得到  11/2 = 5..1
        int totalPages = total / EsConstant.PRODUCT_PAGESIZE == 0 ?
                (int) total / EsConstant.PRODUCT_PAGESIZE
                : ((int) total / EsConstant.PRODUCT_PAGESIZE) + 1;
        result.setTotalPages(totalPages);

        //7.导航页码
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //8.构建面包屑导航功能
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                //1.分析每一个attrs传过来的查询参数值
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                //navVo.setNavName(s[0]) //12_6.5英寸：4.8英寸  ==》s[0]：属性ID
                R r = productFeignService.AttrInfo(Long.parseLong(s[0]));
                //TODO 属性条件筛选联动
                //在选中属性后，添加面包屑，并且收集当前选中的全部属性ID,用做在前端拦截显示。
                result.getAttrIds().add(Long.parseLong(s[0]));
                //远程调用获得结果
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName() + "：");
                } else {
                    navVo.setNavName(s[0]);
                }
                navVo.setNavValue(s[1]);

                //2.取消了面包屑，跳转到哪个地方，将请求地址的URL里面的当前条件置空
                //拿到所有的查询条件，去掉当前。
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }
        //品牌面包屑
        if(param.getBrandId() != null && param.getBrandId().size() > 0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌：");
            //TODO 远程查询所有品牌
            R r = productFeignService.BrandInfo(param.getBrandId());
            if(r.getCode() == 0){
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brand) {
                    buffer.append(brandVo.getName()+";");
                    replace = replaceQueryString(param, brandVo.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            }
            navs.add(navVo);
        }
        //TODO 分类面包屑：不需要导航取消


        return result;
    }

    private String replaceQueryString(SearchParam param, String value , String key) {
//        String replace = param.get_queryString()
//                .replace("&attrs=" + attr, "")
//                .replace("+","%20"); //浏览器对空格编码和java不一样：差异化处理
        String encode = null;
//        try {
//            encode = URLEncoder.encode(attr, "UTF-8");
            encode = value.replace("+" , "%20");//浏览器对空格编码和java不一样：差异化处理
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        String replace = param.get_queryString().replace("&" + key + "=" + encode, "");
//        if(param.get_queryString().contains(key + "=" + encode)){
//            replace = param.get_queryString().replace("?" + key + "=" + encode, "");
//        }
        return replace;
    }
}
