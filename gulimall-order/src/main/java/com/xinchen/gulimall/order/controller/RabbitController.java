package com.xinchen.gulimall.order.controller;

import com.xinchen.gulimall.order.entity.OrderEntity;
import com.xinchen.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

//    @GetMapping("/sendMq")
//    public String sendMq(@RequestParam(value = "num" , defaultValue = "10") Integer num) {
////1.发送消息：需要rabbitTemplate组件作为工具
//        //在RabbitMQ的自动配置类【RabbitAutoConfiguration】中为我们自动配置了RabbitTemplate
//        String msg = "Hello World!";
//        //2.发送的对象类型消息，转变为JSON
//        for (int i = 0; i < num; i++) {
//            if (i % 2 == 0) {
//                //1.如果发送的消息是一个对象：我们会使用序列化机制，将这个对象写出去。
//                //1.1所以要求对象【实体】必须实现Serializable接口
//                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
//                reasonEntity.setId(1L);
//                reasonEntity.setCreateTime(new Date());
//                reasonEntity.setName("哈哈" + i);
//                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reasonEntity);
//            } else {
//                OrderEntity orderEntity = new OrderEntity();
//                orderEntity.setOrderSn(UUID.randomUUID().toString());
//                rabbitTemplate.convertAndSend("hello-java-exchange", "hello22.java", orderEntity);
//            }
//        }
//        return "ok";
//    }

    @GetMapping("/sendMq")
    public String sendMq(@RequestParam(value = "num" , defaultValue = "10") Integer num) {
//1.发送消息：需要rabbitTemplate组件作为工具
        //在RabbitMQ的自动配置类【RabbitAutoConfiguration】中为我们自动配置了RabbitTemplate
        String msg = "Hello World!";
        //2.发送的对象类型消息，转变为JSON
        for (int i = 0; i < num; i++) {
            if (i % 2 == 0) {
                //1.如果发送的消息是一个对象：我们会使用序列化机制，将这个对象写出去。
                //1.1所以要求对象【实体】必须实现Serializable接口
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setCreateTime(new Date());
                reasonEntity.setName("哈哈" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reasonEntity);
            } else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello22.java", orderEntity);
            }
        }
        return "ok";
    }
}
