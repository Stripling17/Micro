package com.xinchen.seckill.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import com.alibaba.fastjson.JSON;
import com.xinchen.common.exception.BizCodeEnume;
import com.xinchen.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 配置流控后的返回数据 SeckillSentinelConfig
 */


@Configuration
public class SeckillSentinelConfig {


    public SeckillSentinelConfig() {
        WebFluxCallbackManager.setBlockHandler(new BlockRequestHandler() {
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
                R error = R.error(BizCodeEnume.TOO_MANY_REQUEST.getCode(), BizCodeEnume.TOO_MANY_REQUEST.getMsg());
                String errorString = JSON.toJSONString(error);
                Mono<ServerResponse> body = ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(Mono.just(errorString), String.class);
                return body;
            }
        });
    }
}


