package com.xinchen.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.xinchen.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    @Value("${alipay.timeout_express}")
    private String timeoutExpress;

    //在支付宝创建的应用的id
    @Value("${alipay.app_id}")
    private  String app_id;

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCcaddAijCOJsiQhCr4Bw3K6XmCsm3IKPshJIxKG5IYgfqGQFWVizlV7F47Sm2Ezr5byMvYFSfhFt+ckBKuv8p8nfBx8vB9Uwp2PfT03t46Iz9z36z6KYdJBYHiKfbrhqD9shV2339j9nMQet5r1kg/7boXwI9fVy1LHqVmAPOE11Fem1K5Lm3uS8QYBkzQO5I/GmdkMTZ+ejKaV9neLsF4yCqjVBBl8whMjuTwSNpf9yIKos3zXBeX08U9noo2PPqOeTPXhRCVX+41bRUjKXaWj03OniCwZsXU06+h2CgbIQHfn+6ETBbyVPdchk/GegSntE7qfvLsBiVwTXSFFRgRAgMBAAECggEASgHmYFpCaTA+m+p9bkfgJc9cuBN20Etbr63cV0A+Wyw8/oK4O/7ZDSe+2mz37uvntAQJN1+jM+SfvzOIM3j59QbB+xiI47OD8riUC7zcB0QgRNJjxSYbJdjeQMW8WGqmCAPYFhvf6ct1XCnWzc1g+Caic5SgAet8udtxRRdUIdkEI2l1uu1UMR0Ud6614HSbQnlxrWZ71KVHJxhiQjI/Ran+77NsSs2bvF1Sw7zzHIlaGNtF0VXc6mQXfe8aiQKKDxkN5L7aNfp+YDVoa2fXVxdUKm2gtxCUUimOTx0W6iFJGnZ4bqWYl+GiTY9xb7hZ0CLyWLFBkMHG9vKN2cVOZQKBgQDmPl75GT9avQmUy1oC9VK9BBnSns25ruwsTg3xsIaslgrUWAaP3oTrQvGaA6Uv7R1aZBj8bpoN73CB+NllhtQWvmY3OuUWoEPdRNx7CtGp/djNx1AJho5wT6Wi17+EPsKsPFabBUIQQ/4UJFldECxqsb3gh320Ssl6CVGPaLPr1wKBgQCt6SYkJC5SFrTAix4OI/faMyD5ob9hJRt8tLoKb0B+bUi0Tc87T8eNBqI2RKfFJtZB+zaz+6iiCe2rhV/AZxTJ8UkpGFJZRUIShNz6al5/JHWVa8Uce46mJ80RFcoHhXv0NN7SFnKoDPSrGqOOqyjkbl+e9TMS3+tg1uCDOIFeVwKBgQCnopm7e+powmILd9NdfAh1nlq+wui+XOGPkHUOl5w2ZDgWStDcRwM311HY9PZ8YoOQoHigslZv9vlPaEPAa95XgOEjLTpGswE2RUDSRRpKToxfptJbXSCwHQ1X9wV138wVYwkbbUSgWyDOwXhfDbuJI0hABeY8Modm1woPBAibmQKBgGx7oGgxDZf1RAFLho8JjsVU2X/+jrQZgqDy145EwbVblFusodV8uZzWA3/YqdLInCSCgSgalqGlkdVJmGYbpxMlaR6yZkP7ePC2YmXuzk1/P73agRV4WY56C2hZ8DFq6dx20nu4twLvOfw13MrVJ+f+lbVBfP+MIHbC8z/HkqN3AoGAQvXuUvSQ+tjyct4YVqOACW7nLXuzZDysPMwwfUYvCrdTz2OsgUGWLrn5jwfDppx07sivVLNv9CA0IBmD7ZK4ROf6wFZ69RGRBaR6oN+NzD2N4UrRYJrVYKJZha7Ye+1HYCTLygz1Gll5xWlS0RBQDsE6Sj3qf+wtpEeP4t0vUAc=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1CV46dTyKfB+io6iObJAS/ZrsEX5vdctQmVVtfzHIT4CBWKHbjxesHxIl4y3aQlmD855V+UnaJQl8wbaLqMsy8ozRUhBcpf8eZiJ7xRRKF7yY6w67XOsvoqqctxl+UMY/RxkwvzgBMKjvhpprrMVcrrcJgjpY1X/+qIo50fPxKuQRi76ftDWxRVo8hWVsBIl6dM1MMTpb+tnPuLplDaIWBLduiIpDIKVXZ+CKuwF5nYEXTRfT/dLzrX2zE+jjgAL5arCFr637vYjHW4dhsS1JDI9J3HKfglRJ7Em101TOIMzen3ehxjpQnfaB/r42SWHnZYgT+jM0eiyu3Jkz0iVDQIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息。
    // 最大努力通知方案一直发送消息通知。
    @Value("${alipay.notify_url}")
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    @Value("${alipay.return_url}")
    private  String return_url;

    // 签名方式
    @Value("${alipay.sign_type}")
    private  String sign_type;

    // 字符编码格式
    @Value("${alipay.charset}")
    private  String charset;

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    @Value("${alipay.gatewayUrl}")
    private  String gatewayUrl;


    public String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+ timeoutExpress +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
