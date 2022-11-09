package com.xinchen.gulimall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;


/**
 * 1.导入依赖
 * 2.编写配置、给容器中注入一个RestHighLevelClient
 * 3.参照API：https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.5/java-rest-high.html
 */
@Configuration
public class GulimallElasticSearchConfig {

    /**
     * RequestOptions:请求的设置项
     * 例如：ES中添加了安全访问规则-->所有的请求都需要带上一个安全的头和其他一些设置信息
     */
    //把RequestOptions实例作为一个单例：以后的所有请求都共享来使用
    public static final RequestOptions COMMON_OPTIONS;
    static {
        //构建一个build
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        //addHeader添加一个请求头信息：加上了一个令牌
//        builder.addHeader("Authorization", "Bearer " + TOKEN);
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory
//                        .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    @Bean
    public RestHighLevelClient esRestClient() {
        RestClientBuilder builder = null;
        // InetAddress address, int port, String scheme
        builder = RestClient.builder(new HttpHost("192.168.56.10", 9200, "http"));
        RestHighLevelClient client =  new RestHighLevelClient(builder);

//        RestHighLevelClient client = new RestHighLevelClient(
//            RestClient.builder(
//                new HttpHost("192.168.56.10", 9200, "http")));

        return client;
    }
}
