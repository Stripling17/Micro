package com.xinchen.gulimall;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinchen.gulimall.product.entity.BrandEntity;
import com.xinchen.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

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

}
