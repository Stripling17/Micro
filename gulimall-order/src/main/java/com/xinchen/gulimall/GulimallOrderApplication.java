package com.xinchen.gulimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用RabbitMQ：
 *  1.引入了amqp场景启动器：RabbitAutoConfiguration【Rabbit配置自动生效】
 *  2.给容器中自动配置啦：RabbitTemplate AmqpAdmin 还有连接工厂rabbitConnectionFactory 与rabbitMessagingTemplate
 *      所有的属性都是以：spring.rabbitmq 来开头
 *          2) @ConfigurationProperties(prefix = "spring.rabbitmq")
 *              public class RabbitProperties(){//在这个类进行了rabbitMQ的属性绑定}
 *  3.给以 spring.rabbitmq 来开头属性配置文件：
 *  4.@EnableRabbit:@EnableXXX
 *  5.监听消息：使用@RabbitListener；必须有@EnableRabbit才能使用
 *
 *   @RabbitListener：类+方法上 (监听那些队列即可)
 *      如果一个队列中有两种不同类型的消息。在监听消息使用content的时候，获取到不同类型的消息无法正确转换
 *  @RabbitHandler：标在方法上  （重载区分不同的消息）
 *      将@RabbitListener放在类上监听队列。@RabbitHandler放在方法上，根据接收参数类型的不同，区分不同方法监听队列中哪一个对象类型的数据
 *
 * 使用@RabbitHandler标注了了各种不同的接收消息方法。 ==》RabbitHandler进行重载处理
 *      可以监听多个队列，每个队列可能消息不一样
 *      或者多一个队列中消息也不一样
 *
 * 6.RabbitMQ消息确认机制-可靠抵达
 *  生产端确认：ConfirmCallback：确认模式 生产者给消息代理服务器发送消息后，消息代理返回的回调
 *  队列确认：returnCallback：退回模式  交换机给队列的消息成功投递后，队列向交换机返回的回调
 *
 * 7.分布式事务问题（本地事务下的问题）
 *  问题原因：1.远程调用假失败：在调用远程服务后，远程服务正确完成业务，但是由于机器慢，故障，卡死，服务器故障等
 *          各种原因。本地事务完成并提交以后，一直没给主服务返回。远程调用的超时机制误判远程调用出现异常。导致
 *          远程调用服务正确执行完整事务，但是主服务却因异常机制回滚。
 *          2.一个业务中，有多个远程调用，如果后面的远程调用出现异常并回滚。只能回滚主服务中的事务，先前远程调用
 *          的服务已经完成完整事务，无法回滚。解决方法，手动回滚
 *          4.一个业务中远程服务执行完成，但是下面的其他方法出现问题，导致异常回滚。
 *              但是异质性的远程请求，肯定不能回滚
 *
 * 8.本地事务失效问题：
 *      同一个对象内事务方法互调默认失效，原因，绕过了代理对象，事务使用代理对象来控制的：事务可以加上，事务的设置没有用
 *      解决：使用代理对象来调用事务方法
 *          1）映入aop-starter==》spring-boot-starter-aop -> aspectjweaver
 *          2）开启aspectjweaver动态代理功能: @EnableAspectJAutoProxy
 *              以后所有的动态代理都是aspectj创建的（不需要像之前JDK动态代理一样需要接口才能代理）
 *          3）对外暴露代理对象：(exposeProxy = true)
 *      使用：本类互调用调用【代理】对象
 *
 * 9.Seata控制分布式事务
 *  1）、每一个微服务先必须创建undo_log表
 *  2）、安装事务协调器：seata-server
 *  3）、整合
 *      1.导入依赖： spring-cloud-starter-alibaba-seata 2021.0.4.0 io.seata.all 1.5.2
 *      2.解压并启动seata-server 1.5.2
 *          修改seata的nacos注册中心与配置中心配置
 *      3.所有想要用到分布式事务的微服务都应该使用seata DataSourceProxy代理自己的数据源
 *      4.每一个微服务，都必须导入 gistry.conf file.conf
 *      5.微服务file.conf中：vgroupMapping.${spring.application.name}-fescar-service-group = "default"
 *      6.给分布式大事务（business）的入口标注@GlobalTranscatioinal 每一个远程的小事务用Transactional
 *      7.启动测试分布式任务
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableRedisHttpSession
@EnableRabbit
@EnableFeignClients("com.xinchen.gulimall.order.feign")
@MapperScan("com.xinchen.gulimall.order.dao")
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
