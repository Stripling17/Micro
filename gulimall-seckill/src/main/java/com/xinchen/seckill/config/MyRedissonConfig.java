package com.xinchen.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {

    /**
     * 所有对redis的使用都是通过RedissonClient对象
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        Config config = new Config();
        //1.创建配置
        //Redis url should start with redis:// or rediss://
        config.useSingleServer()
                //可以用"rediss://"来启用SSL连接 ===>rediss===>安全连接
                .setAddress("redis://192.168.56.10:6379");

        //2.根据config对象创建 RedissonClient 实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
