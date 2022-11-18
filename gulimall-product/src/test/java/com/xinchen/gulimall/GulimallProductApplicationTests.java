package com.xinchen.gulimall;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinchen.gulimall.product.dao.AttrGroupDao;
import com.xinchen.gulimall.product.dao.SkuSaleAttrValueDao;
import com.xinchen.gulimall.product.entity.BrandEntity;
import com.xinchen.gulimall.product.service.BrandService;
import com.xinchen.gulimall.product.service.CategoryService;
import com.xinchen.gulimall.product.vo.SkuItemSaleAttrVo;
import com.xinchen.gulimall.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient RedissonClient;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    public void getSaleAttrsBySpuId(){
        List<SkuItemSaleAttrVo> skuItemSaleAttrVos= skuSaleAttrValueDao.getSaleAttrsBySpuId(1L);
        System.out.println(skuItemSaleAttrVos);
    }

    @Test
    public void getAttrGroupWithAttrsBySpuId(){
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(225L, 1L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }

    @Test
    public void redisson() {
        System.out.println(RedissonClient);
    }

    @Test
    public void testStringRedisTemplate(){
        //Hello World
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        //保存
        ops.set("hello","world_"+ UUID.randomUUID().toString());

        //查询
        String hello = ops.get("hello");
        System.out.println("之前保存的数据是：" + hello);
    }


    @Test
    public void testFindPath(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径：{}", Arrays.asList(catelogPath));
    }

    @Test
    void contextLoads() {

//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setBrandId(1L);
//        brandEntity.setName("苹果");

//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        System.out.println("保存成功");

//        brandService.updateById(brandEntity);
//        System.out.println("修改成功");

        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        list.forEach((item)->{
            System.out.println(item);
        });
    }
        @Test
        public void test() {
        }


    }
