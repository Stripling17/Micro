package com.xinchen.gulimall.product.service.impl;

import com.xinchen.gulimall.product.vo.SkuItemSaleAttrVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.product.dao.SkuSaleAttrValueDao;
import com.xinchen.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.xinchen.gulimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     *## 传入了spu的ID，
     * ## 分析当前spu有多少个sku，所有sku涉及到的属性组合
     * ## info.sku_id,
     * SELECT
     * 	ssav.attr_id,
     * 	ssav.attr_name,
     * 	GROUP_CONCAT(DISTINCT ssav.attr_value)
     * FROM pms_sku_info info
     * LEFT JOIN
     * pms_sku_sale_attr_value ssav
     * ON ssav.sku_id = info.sku_id
     * WHERE info.spu_id = 1
     * GROUP BY ssav.attr_id,ssav.attr_name
     */
    @Override
    public List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId) {
        List<SkuItemSaleAttrVo> saleAttrVos = this.baseMapper.getSaleAttrsBySpuId(spuId);
        return saleAttrVos;
    }

    @Override
    public List<String> getSkuSaleAttrValues(Long skuId) {
        return this.baseMapper.getSkuSaleAttrValues(skuId);
    }

}
