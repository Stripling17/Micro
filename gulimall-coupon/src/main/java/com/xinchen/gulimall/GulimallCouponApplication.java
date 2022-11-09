package com.xinchen.gulimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 如何使用Nacos作为配置中心统一管理配置
 * 1）引入依赖
 *         <dependency>
 *             <groupId>com.alibaba.cloud</groupId>
 *             <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
 *         </dependency>
 * 2）创建一个bootstrap.properties -->失败-->导入spring-cloud-starter-bootstrap
 *  1、spring.application.name=gulimall-coupon
 *     spring.cloud.nacos.config.server-addr=127.0.0.1:8848
 * 3）需要给配置中心默认添加一个叫 数据集（Data Id)gulimall-coupon.properties
 *      默认规则：应用名（服务名）+properties
 * 4）给应用名.properties添加任何配置
 * 5）动态获取配置
 *      @RefreshScope:动态获取并刷新配置
 *      @Value("${coupon.user.name}") coupon.user.name--->配置项的名称
 * 注意：如果配置中心和当前应用的配置文件中都配置了相同的项，优先使用配置中心的配置
 *
 * 2、细节
 * 1）、命名空间：配置隔离
 *      默认的命名空间：public（保留空间） 默认新增的所有配置都在public保留空间下
 *      命名空间的作用是用来做配置(环境)隔离的：
 *          隔离：例如开发、测试、生产；
 *          注意：在bootstraps.properties；配置上，需要使用那个命名空间的配置
 *      spring.cloud.nacos.config.namespace=56b894e9-4804-4d92-9a47-1aefe0219212
 *      2.每一个微服务之间互相隔离配置：每一个微服务都创建自己命名空间，只加载自己命名空间下的所有配置
 *
 * 2）、配置集：所有的配置集合
 * 3）、配置ID：配置文件名
 *      Data Id：类似文件名
 * 4）、配置分组：
 *      默认所有的配置集都属于：DEFAULT_GROUP
 *      例如双11 618 双12 可以用不用的配置分组 例如： 1111 618 1212
 *
 * 本项目使用：
 *      每个微服务创建自己的命名空间，再来通过是使用配置分组来区分它的环境,dev、test、prod
 *
 * 3.同时加载多个配置集
 *      1）、微服务任何配置信息，任何配置文件都可以放在配置中心中
 *      2）、只需要在bootstrap.properties说明加载配置中心的哪些配置文件即可
 *      3）、通过注解@Value与@ConfigurationProperties...来获取配置文件中的配置
 *   以前的SpringBoot任何方法从配置文件中获取值，都能使用。
 *   使用规则：配置中心有的，默认使用配置中心中的值。
 */

/**
 * 优惠服务
 */
@EnableFeignClients(basePackages = "com.xinchen.gulimall.coupon.feign")
@MapperScan("com.xinchen.gulimall.coupon.dao")
@SpringBootApplication
@EnableDiscoveryClient
//开启服务注册与发现功能 -->开启发现客户端
public class GulimallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCouponApplication.class, args);
    }

}
