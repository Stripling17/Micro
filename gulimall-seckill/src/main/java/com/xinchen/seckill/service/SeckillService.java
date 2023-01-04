package com.xinchen.seckill.service;

import com.xinchen.seckill.to.SeckillSKuRedisTo;

import java.util.List;

public interface SeckillService {

    void uploadSeckillSkuLatest3Days();

    //返回当前时间可以参与秒杀的商品信息
    List<SeckillSKuRedisTo> getCurrentSeckillSkus();

    SeckillSKuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
