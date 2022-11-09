package com.xinchen.gulimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
 * 2.逻辑删除
 *      1）配置全局的逻辑规则（省略）
 *      2）配置逻辑删除的组件Bean（Mybatis-Plus 3.1.0version以上可以省略）
 *      3）给需要删除的实体类字段【Bean】上加上@TableLogic注解
 *
 * 3.JSR303
 *      1)给springboot加入spring-boot-starter-validation依赖
 *      2)给Bean添加校验注解：jakarta.validation-api中javax.validation.constraints包下的依赖
 *      3)自定义message提示，并开启校验功能@Valid 效果：校验错误后会有默认的响应；
 *      4）给校验的bean后紧跟一个BindingResult,就可以获取到校验的结果
 *      5）分组校验[多场景的复杂校验]
 *          1.@NotBlank(message = "品牌名必须提交" , groups = {AddGroup.class,UpdateGroup.class})
 *          给校验注解标注什么情况需要校验
 *          2.@Validated({AddGroup.class})
 *          3.默认没有指定分组的校验注解。例如：@NotNull，在分组校验情况下不生效，只会在不分组的校验下生效：@validated(value={null})
 *
 *      6）自定义校验
 *          1.编写一个自定义的校验注解
 *          2.编写一个自定义的校验器
 *          3.关联自定义的校验和自定义的校验注解标注的字段
 *          @Documented
 *          @Constraint(validatedBy = {ListValueConstraintValidator.class【可以指定多个不同的校验器，适配不同类型的校验】})
 *          @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
 *          @Retention(RUNTIME)
 *
 * 4.统一的异常处理
 * @ControllerAdvice
 * 1)编写异常处理类，使用ControllerAdvice。
 * 2）使用@ExceptionHandler标注方法可以处理的异常。
 *
 * 5.模板引擎
 *  1）thymeleaf-starter: 关闭缓存
 *  2)静态资源都放在static文件夹下就可以按照路径直接访问
 *  3)页面放在template模板引擎下，直接访问
 *      springboot访问项目的时候，默认会找index
 *  4）修改页面不重启服务器：实时更新
 *      引入SpringBoot为我们提供的dev-tools工具
 *
 * 6.整合redis
 *  1）、引入data-redis-starter
 *  2）、简单配置redis的host等信息
 *  3）、使用SpringBoot自动配置好stringRedisTemplate来操作redis <String【name】,String【Json】>
 *      redis -> Map:存放数据key，数据值value
 *
 * 7.整合redisson作为分布式锁等功能框架
 *  1）、引入依赖
 * <dependency>
 *     <groupId>org.redisson</groupId>
 *     <artifactId>redisson</artifactId>
 *     <version>3.12.0</version>
 * </dependency>
 * 2）、配置redisson：MyRedissonConfig给容器中配置一个RedissonClient实例即刻
 *
 * 8.整合springCache简化缓存开发
 *      1）、引入依赖
 *      <artifactId>spring-boot-starter-cache</artifactId>
 *      <artifactId>spring-boot-starter-data-redis</artifactId>
 *      2）、写配置
 *          （1）自动配置了那些
 *              cacheAutoConfiguration会导入RedisCacheConfiguration；
 *              RedisCacheConfiguration.class自动配置好了RedisCacheManager
 *          （2）配置使用redis作为缓存
 *          spring.cache.type=redis
 *      3）、测试使用缓存
 *      （1）开启缓存注解功能：@EnableCaching
 *      （2）只需要使用注解就能完成缓存操作
 *          @Cacheable：在方法执行前spring先查看缓存中是否有数据，如果有数据，则直接返回缓存数据；若没有数据，调用方法并将方法返回值放到缓存中
 *          @CachePut：不影响方法执行更新缓存
 *          @CacheEvict：将一条或多条数据从缓存中删除
 *          @Caching：组合以上多个操作
 *          @CacheConfig：在类级别共享相同的缓存配置
 *      4）原理：
 *      CacheAutoConfiguration -导入-> RedisCacheConfiguration -存在-> RedisCacheManager
 *      -实现-> 初始化配置文件中所配置【cache-names=cache1,c2,c3】全部缓存 -调用-> DetermineConfiguration()
 *      -通过-> 拿到RedisCacheConfiguration缓存配置 -决定-> 我们缓存应该使用哪一个自动配置
 *
 *      CacheAutoConfiguration只提供了一个有参构造器，如果一个组件只有一个有参构造器，那么这个组件里面
 *      所有的参数都来源于容器中：
 *      因此如果我们想要更改缓存的配置，只需要给容器中放一个RedisCacheConfiguration即可
 *      public class MyCacheConfig {
 *          @Bean
 *          RedisCacheConfiguration redisCacheConfiguration() {}
 *      }
 *
 * Spring-Cache的不足
 *      1）、读模式：
 *          缓存穿透：查询一个永不存在（null）的数据。就会一直去查数据库，方案：缓存空数据
 *                  spring.cache.redis.cache-null-values=true
 *          缓存击穿：大量并发进来同事查询一个正好过期的数据。方案：加锁？ spring-cache默认是没有加锁的
 *          sync = true 加锁解决击穿【使用的是本地锁，不是分布式锁】
 *          缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间
 *      2）、写模式：（缓存与数据库一致）
 *          （1）读写加锁：有序进行。适用于读多写少的系统，并且在特定情况下可以舍弃实时性，和强一致性
 *          （2）映入Canal：感知到Mysql的更新去更新数据库
 *          （3）读多，写多的，直接去数据库查询就行
 *
 *      总结：
 *          常规数据（读多写少，即时性和一致性要求不高的数据）   反正有缓存过期时间：因此完全可以使用SpringCache
 *          特殊数据：特殊数据 单独使用Canal，读写锁，或者排队的公平锁，阻塞
 *
 * 原理：
 *      CacheManager(RedisCacheManager) -创建-> Cache组件(RedisCacheManager) -负责-> 缓存的读写
 *
 */
@EnableCaching
//Mybatis的配置 mapper包扫描
@EnableFeignClients(basePackages = "com.xinchen.gulimall.product.feign")
@MapperScan("com.xinchen.gulimall.product.dao")
@SpringBootApplication
//开启服务注册功能
@EnableDiscoveryClient
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
