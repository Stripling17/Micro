package com.xinchen.gulimall;

import com.xinchen.gulimall.order.entity.OrderEntity;
import com.xinchen.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
//@RunWith(Runner.class)
@SpringBootTest
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 生产者：
     *   public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {
     *      @Nullable
     *      T getIfUnique() throws BeansException;
     *   }
     *    ObjectProvider<T>：说明T类型数据从容器中获取，如果容器中有，就将他放入到RabbitTemplate中去；
     *    如果容器中没有：那么默认使用RabbitTemplate中的new SimpleMessageConverter()类;
     *
     *    SimpleMessageConverter这个类中，有一个createMessage方法：
     *          如果传输的是String类型，就直接返回
     *          如果是一个Object，就将对象序列化成byte[]数组，然后传输
     *
     *    我们需要在容器中为MessageConverter装配一个Jackson2JsonMessageConverter类，放入到template中。
     *    就能实现消息中间件获取到对象的JSON数据
     *
     *
     */
    @Test
    public void sendMessageTest() {

        //1.发送消息：需要rabbitTemplate组件作为工具
        //在RabbitMQ的自动配置类【RabbitAutoConfiguration】中为我们自动配置了RabbitTemplate
        String msg = "Hello World!";
        //2.发送的对象类型消息，转变为JSON
        for (int i = 0; i < 10; i++){
            if(i%2 == 0){
                //1.如果发送的消息是一个对象：我们会使用序列化机制，将这个对象写出去。
                //1.1所以要求对象【实体】必须实现Serializable接口
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setCreateTime(new Date());
                reasonEntity.setName("哈哈"+i);
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",reasonEntity);
            }else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderEntity);
            }

            log.info("消息发送完成{}");
        }
    }

    /**
     * 1.如何创建Exchange[hello.java.exchange]，Queue、Binding
     *      1）AmqpAdmin：高级消息队列管理的组件  ==》来创建交换机和队列
     *      2) @ConfigurationProperties(prefix = "spring.rabbitmq")
     *          public class RabbitProperties(){//在这个类进行了rabbitMQ的属性绑定}
     * 2.如何收发消息
     */
    @Test
    public void createExchange() {
        //amqpAdmin管理组件：创建交换机，创建队列，绑定关系等等，包括销毁这些队列
        //declareExchange(Exchange exchange) == Exchange接口
//        public DirectExchange(String name, //名字
//                boolean durable, //交换机是否持久化:true 如果重新启动消息服务：不会清空已持久的交换机
//                boolean autoDelete, //是否自动删除：false：如果交换机没有绑定队列：也不会删除交换机
//                Map<String, Object> arguments) //可以在交换机创建的时候，指定一些参数
        DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功","hello-java-exchange");
    }

    /**
     * 创建队列
     */
    @Test
    public void createQueue() {
        //public Queue(String name, boolean durable, boolean exclusive, boolean autoDelete)
        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功","hello-java-queue");
    }

    @Test
    public void createBinding() {
        //String destination【目的地】,
        //Binding.DestinationType destinationType【目的地的类型】,
        //String exchange【交换机】,
        //String routingKey【路由键】,
        //@Nullable Map<String, Object> arguments【自定义参数】
        /**
         * 将exchange指定的交换机和destination目的地进行绑定，使用routingKey作为指定的路由键
         */
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE,"hello-java-exchange","hello.java",null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功","hello-java-Binding");
    }

}
