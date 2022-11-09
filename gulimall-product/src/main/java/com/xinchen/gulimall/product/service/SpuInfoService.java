package com.xinchen.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.product.entity.SpuInfoDescEntity;
import com.xinchen.gulimall.product.entity.SpuInfoEntity;
import com.xinchen.gulimall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity infoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    //商品上架
    void up(Long spuId);
}

