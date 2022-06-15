package com.xinchen.gulimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1、整合Mybatis-plus
 *      1）、导入依赖
 *      <dependency>
 *           <groupId>com.baomidou</groupId>
 *           <artifactId>mybatis-plus-boot-starte</artifactId>
 *           <version>3.2.0</version>
 *      </dependency>
 *      2、配置
 *          1.配置数据源；
 *              1）导入数据库的驱动。
 *              2）在application.yml配置数据源相关信息
 *          2.配置mybatis-plus；
 *              1）使用MapperScan
 *              2）告诉Mybatis-plus sql映射文件位置
 *
 */
//Mybatis的配置 mapper包扫描
@MapperScan("com.xinchen.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
