package com.xinchen.seckill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 1.做好消息确认机制(publisher,consumer[手动ACK])
 * 2.每一个发送的消息都在数据库做好记录。定期将以各种原因失败的消息再次发送
 *
 * 重复：1.即将ACK的时候宕机
 *      2.由于消费重试机制，会将消费失败的消息又发送出去 try(){}catch(){发送给Broker}
 *      3.解决方法：
 *          （1）消费者的业务接口应该设计为幂等性：在数据库表中设置已经消费成功的标识位，如果重复，下次不会处理
 *          （2）或者使用防重表，将处理过的订单号放在里面
 *          （3）rabbitMQ发送的没一个消息都有一个redelivered字段，可以获取是否被
 *              重新投递过来。我们可以针对重新投递过来的数据进行处理。再次消费，消费认证，日志记录。
 *
 * 积压：1.消费者宕机积压
 *      2.消费者消费能力不足积压
 *      3.发送者发送流量太大
 *          上线更多的消费者，进行正常的消费
 *          上线专门的队列消息服务，将消息先批量取出来，记录数据库，离线慢慢处理
 *
 */
@Slf4j
@Configuration
public class MyRabbitConfig {

    //@Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

//    @Bean
//    public ConnectionFactory connectionFactory() {
//        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
//        cachingConnectionFactory.setHost("192.168.56.10");
//        cachingConnectionFactory.setUsername("guest");
//        cachingConnectionFactory.setPassword("guest");
//        cachingConnectionFactory.setPort(5672);
//        cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
//        cachingConnectionFactory.setPublisherReturns(true);
//        return cachingConnectionFactory;
//    }


    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        //RabbitTemplate rabbitTemplate = new RabbitTemplate(this.connectionFactory());
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        initRabbitTemplate();
        return rabbitTemplate;
    }

    /**
     * 定制RabbitTemplate
     * 1、代理服务器收到消息进行回调
     *      1.spring.rabbitmq.publisher-confirm-type=correlated
     *      2.设置确认回调ConfirmCallback
     * 2、消息正确抵达队列进行回调
     *      1.  spring.rabbitmq.publisher-confirms=true
     *          spring.rabbitmq.publisher-confirm-type=correlated
     *      2.  设置确认回调ReturnCallback
     * 3.消费端确认（保证每个消息被正确消费，此时才可以Broker删除这个消息）
     *      1、默认是自动确认的，只要消息接收到，服务端就会移除这个消息
     *      问题：我们收到一个消息，默认的自动回复机制回复给服务器ack，只有一个消息处理成功==》宕机啦
     *          引发消息丢失
     *      方案：手动确认模式manual：只要我们没有告诉【回复】rabbit消息已被签收。相当于没有Ack。
     *           消息就一直都是unAcked状态，即使Consumer宕机，消息不会丢失，会重新变为Ready状态。
     *           下一次有新的Consumer进来，就会发给她
     *       2、如何签收
     *          签收货物：channel.basicAck(deliveryTag,false);
     *          拒绝签收：channel.basicNack(deliveryTag,false,false); 传递标识，批量拒收，退回队列
     */
    //@PostConstruct //MyRabbitConfig对象创建完成以后，执行这个方法（在构造器构造以后）
    public void initRabbitTemplate() {
        //自定义确定回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 1.只要消息抵Broker代理服务器就ack=true
             * @param correlationData 当前消息的唯一关联数据：主要就是一个消息ID（唯一）
             *     public CorrelationData(String id) {
             *         Assert.notNull(id, "'id' cannot be null and must be unique");
             *         this.id = id;
             *     }
             * @param b Ack:消息是否成功收到
             * @param s cause:失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("confirm...correlationData["+correlationData+"]==>ack["+b+"]==>cause["+s+"]");
            }
        });

        //第二处确认回调：消息抵达队列后的确认回调
//        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
//            /**
//             * 触发时机：只要消息没有投递给指定的队列，就触发这个失败回调
//             * @param returnedMessage
//             *        message: 投递失败的详细信息
//             *        replyCode: 回复的状态码
//             *        replyText: 回复的文本内容
//             *        exchange:  当时这个消息发给哪个交换机
//             *        routingKey 当时这个消息指定的哪个路由键
//             */
//            @Override
//            public void returnedMessage(ReturnedMessage returnedMessage) {
//                System.out.println("FailMessage==>" + returnedMessage);
//            }
//        });

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                System.out.println("message=="+message);
            }
        });


//        //第二处确认回调：消息抵达队列后的确认回调
//        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
//            /**
//             * 触发时机：只要消息没有投递给指定的队列，就触发这个失败回调
//             * @param returnedMessage
//             *        message: 投递失败的详细信息
//             *        replyCode: 回复的状态码
//             *        replyText: 回复的文本内容
//             *        exchange:  当时这个消息发给哪个交换机
//             *        routingKey 当时这个消息指定的哪个路由键
//             */
//            @Override
//            public void returnedMessage(ReturnedMessage returnedMessage) {
//                System.out.println("Fail Message["+returnedMessage.getMessage()+"]==>replyCode["+ returnedMessage.getReplyCode()+"]" +
//                        "==>replyText["+ returnedMessage.getReplyText()+"]==>exchange["+ returnedMessage.getExchange()+"]==>routingKey["+ returnedMessage.getRoutingKey()+"]");
//            }
//        });

    }
}
