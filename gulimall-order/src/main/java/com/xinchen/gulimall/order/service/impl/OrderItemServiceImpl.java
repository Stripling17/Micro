package com.xinchen.gulimall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xinchen.gulimall.order.entity.OrderEntity;
import com.xinchen.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.order.dao.OrderItemDao;
import com.xinchen.gulimall.order.entity.OrderItemEntity;
import com.xinchen.gulimall.order.service.OrderItemService;


@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues:声明需要监听的所有队列
     * 参数可以写以下类型
     * 1.Message m 原生消息类型：包括消息头；消息体
     * 2.T<发送消息的类型> OrderReturnReasonEntity content ==> spring会为我们自动转化
     * 3.Channel channel ：当前传输数据的通道
     *          一个客户端只和消息中间件建立一条长连接；连接中有很多管道【Channel】
     *
     * Queue:可以很多人都来监听。只要收到消息，队列就会删除消息，而且只能有一个人收到此消息
     * 场景：
     *      1）、订单服务启动多个:同一个消息只能有一个客户端收到
     *      2）、只有一个消息完全处理完，方法运行结束，我们就可以接收到下一个消息
     */

//    @RabbitListener(queues = {"hello-java-queue"})
    @RabbitHandler
    public void reciveMessage(Message message,
                              OrderReturnReasonEntity content,
                              Channel channel){
        //class org.springframework.amqp.core.Message
        //body = '{"id":1,"name":"哈哈哈哈","sort":null,"status":null,"createTime":1658160773440}'
//        System.out.println("接收到消息...内容："+message+"===>内容："+content);
        System.out.println("消息体以对象OrderReturnReasonEntity接收："+content.getName());
        byte[] body = message.getBody();
        //JSON.parseObject()
        //消息头属性信息
        MessageProperties messageProperties = message.getMessageProperties();
//        Thread.sleep(3000);
//        System.out.println("消息处理完成=>" + content.getName());
        //传递标签：Channel内按照顺序自增的
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("deliveryTag==>"+deliveryTag);
        //签收货物
        try {
            if(deliveryTag%2 == 0){
                //收货
                channel.basicAck(deliveryTag,false);
                System.out.println("签收了货物..."+deliveryTag);
            }else {
                //退货  boolean var4 =false 丢弃 丢弃模式 var4 =true 退还给服务器
                //long var1, boolean var3, boolean var4
                //如果批量拒收，以前的消息全部被拒绝
                channel.basicNack(deliveryTag,false,false);
                //long var1, boolean var3
                //channel.basicReject();
                System.out.println("没有签收了货物..."+deliveryTag);
            }
            channel.basicAck(deliveryTag,false); //channel.basicAck(deliveryTag,false);
            System.out.println("签收了货物..."+deliveryTag);

        } catch (IOException e) {
            //服务器异常或者网络中断
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void reciveMessage2(Message message,
                              OrderEntity content,
                              Channel channel) throws InterruptedException {
        System.out.println("消息体以对象OrderEntity接收："+content.getOrderSn());
    }
}
