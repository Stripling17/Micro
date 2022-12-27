package com.xinchen.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyRabbitConfig {
    //@Autowired
    //RabbitTemplate rabbitTemplate;

//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Message message) {
//
//    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Exchange stockEventExchange() {
        return new TopicExchange("stock-event-exchange", true , false);
    }

    @Bean
    public Queue stockReleaseStockQueue() {
        return new Queue("stock.release.stock.queue", true, false, false);
    }

    @Bean
    public Queue stockDelayQueue() {
        /**
         * x-dead-letter-exchange: stock-event-exchange
         * x-dead-letter-routing-key: order.release.order
         * x-message-ttl: 6000
         */
        Map<String,Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","stock-event-exchange");
        arguments.put("x-dead-letter-routing-key","stock.release");
        arguments.put("x-message-ttl",120000);

        return new Queue("stock.delay.queue", true, false, false, arguments);
    }

    /**
     * 普通队列
     */
    @Bean
    public Binding stockReleaseBinding() {
        return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE,"stock-event-exchange",
                "stock.release.#",null);
    }

    /**
     *延时队列
     */
    @Bean
    public Binding stockLockedBinding() {
        return new Binding("stock.delay.queue", Binding.DestinationType.QUEUE,"stock-event-exchange",
                "stock.locked",null);
    }

}
////    @Bean
////    public ConnectionFactory connectionFactory() {
////        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
////        cachingConnectionFactory.setHost("192.168.56.10");
////        cachingConnectionFactory.setUsername("guest");
////        cachingConnectionFactory.setPassword("guest");
////        cachingConnectionFactory.setPort(5672);
////        cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
////        cachingConnectionFactory.setPublisherReturns(true);
////        return cachingConnectionFactory;
////    }
//
//
//    @Primary
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        //RabbitTemplate rabbitTemplate = new RabbitTemplate(this.connectionFactory());
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        this.rabbitTemplate = rabbitTemplate;
//        rabbitTemplate.setMessageConverter(messageConverter());
//        initRabbitTemplate();
//        return rabbitTemplate;
//    }
//
//    /**
//     * 定制RabbitTemplate
//     * 1、代理服务器收到消息进行回调
//     *      1.spring.rabbitmq.publisher-confirm-type=correlated
//     *      2.设置确认回调ConfirmCallback
//     * 2、消息正确抵达队列进行回调
//     *      1.  spring.rabbitmq.publisher-confirms=true
//     *          spring.rabbitmq.publisher-confirm-type=correlated
//     *      2.  设置确认回调ReturnCallback
//     * 3.消费端确认（保证每个消息被正确消费，此时才可以Broker删除这个消息）
//     *      1、默认是自动确认的，只要消息接收到，服务端就会移除这个消息
//     *      问题：我们收到一个消息，默认的自动回复机制回复给服务器ack，只有一个消息处理成功==》宕机啦
//     *          引发消息丢失
//     *      方案：手动确认模式manual：只要我们没有告诉【回复】rabbit消息已被签收。相当于没有Ack。
//     *           消息就一直都是unAcked状态，即使Consumer宕机，消息不会丢失，会重新变为Ready状态。
//     *           下一次有新的Consumer进来，就会发给她
//     *       2、如何签收
//     *          签收货物：channel.basicAck(deliveryTag,false);
//     *          拒绝签收：channel.basicNack(deliveryTag,false,false); 传递标识，批量拒收，退回队列
//     */
//    //@PostConstruct //MyRabbitConfig对象创建完成以后，执行这个方法（在构造器构造以后）
//    public void initRabbitTemplate() {
//        //自定义确定回调
//        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
//            /**
//             * 1.只要消息抵Broker代理服务器就ack=true
//             * @param correlationData 当前消息的唯一关联数据：主要就是一个消息ID（唯一）
//             *     public CorrelationData(String id) {
//             *         Assert.notNull(id, "'id' cannot be null and must be unique");
//             *         this.id = id;
//             *     }
//             * @param b Ack:消息是否成功收到
//             * @param s cause:失败的原因
//             */
//            @Override
//            public void confirm(CorrelationData correlationData, boolean b, String s) {
//                System.out.println("confirm...correlationData["+correlationData+"]==>ack["+b+"]==>cause["+s+"]");
//            }
//        });
//
//        //第二处确认回调：消息抵达队列后的确认回调
////        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
////            /**
////             * 触发时机：只要消息没有投递给指定的队列，就触发这个失败回调
////             * @param returnedMessage
////             *        message: 投递失败的详细信息
////             *        replyCode: 回复的状态码
////             *        replyText: 回复的文本内容
////             *        exchange:  当时这个消息发给哪个交换机
////             *        routingKey 当时这个消息指定的哪个路由键
////             */
////            @Override
////            public void returnedMessage(ReturnedMessage returnedMessage) {
////                System.out.println("FailMessage==>" + returnedMessage);
////            }
////        });
//
//        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
//            @Override
//            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
//                System.out.println("message=="+message);
//            }
//        });
//
//
////        //第二处确认回调：消息抵达队列后的确认回调
////        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
////            /**
////             * 触发时机：只要消息没有投递给指定的队列，就触发这个失败回调
////             * @param returnedMessage
////             *        message: 投递失败的详细信息
////             *        replyCode: 回复的状态码
////             *        replyText: 回复的文本内容
////             *        exchange:  当时这个消息发给哪个交换机
////             *        routingKey 当时这个消息指定的哪个路由键
////             */
////            @Override
////            public void returnedMessage(ReturnedMessage returnedMessage) {
////                System.out.println("Fail Message["+returnedMessage.getMessage()+"]==>replyCode["+ returnedMessage.getReplyCode()+"]" +
////                        "==>replyText["+ returnedMessage.getReplyText()+"]==>exchange["+ returnedMessage.getExchange()+"]==>routingKey["+ returnedMessage.getRoutingKey()+"]");
////            }
////        });
