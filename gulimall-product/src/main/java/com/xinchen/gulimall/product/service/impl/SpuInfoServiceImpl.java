package com.xinchen.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.xinchen.common.constant.ProductConstant;
import com.xinchen.common.to.MemberPrice;
import com.xinchen.common.to.SkuHasStockVo;
import com.xinchen.common.to.SkuReductionTo;
import com.xinchen.common.to.SpuBoundTo;
import com.xinchen.common.to.es.SkuEsModel;
import com.xinchen.common.utils.R;
import com.xinchen.gulimall.product.dao.SpuInfoDescDao;
import com.xinchen.gulimall.product.entity.*;
import com.xinchen.gulimall.product.feign.CouponFeignService;
import com.xinchen.gulimall.product.feign.SearchFeignService;
import com.xinchen.gulimall.product.feign.WareFeignService;
import com.xinchen.gulimall.product.service.*;
import com.xinchen.gulimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1.保存Spu基本信息：pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        //2.保存Spu的描述图片：pms_spu_images
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",",decript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        //3.保存Spu的图片集：pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveSpuImages(infoEntity.getId(),images);

        //4.保存Spu的规格参数：pms_sku_sale_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
            attrValueEntity.setAttrId(attr.getAttrId());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            attrValueEntity.setAttrName(attrEntity.getAttrName());
            attrValueEntity.setAttrValue(attr.getAttrValues());
            attrValueEntity.setQuickShow(attr.getShowDesc());
            attrValueEntity.setSpuId(infoEntity.getId());

            return attrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        //5.保存Spu的积分信息：gulimall_sms -> sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r.getCode() != 0){
            log.error("远程保存spu积分信息失败");
        }


        //6.保存当前spu对应的所有sku的信息
        List<Skus> skus = vo.getSkus();

        if(skus != null && skus.size() > 0){
            for (Skus sku : skus) {
                String defaultImg = "";
                for (Images image : sku.getImages()) {
                    if(image.getDefaultImg() == 1){
                        defaultImg = image.getImgUrl();
                    }
                }
//                private String skuName;
//                private BigDecimal price;
//                private String skuTitle;
//                private String skuSubtitle;
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku,skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                List<Images> images1 = sku.getImages();
                //6.1 保存Sku的基本信息：pms_sku_info
                skuInfoService.saveSkuInfo(skuInfoEntity);


                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> imagesEntities = images1.stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity ->{
                    //返回true就是需要，返回false就是过滤掉【剔除】
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                //6.2 保存Sku的图片信息：pms_sku_images
                skuImagesService.saveBatch(imagesEntities);
                //TODO 没有图片，路径无需保存


                List<Attr> attrs = sku.getAttr();
                List<SkuSaleAttrValueEntity> attrValueEntities = attrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity saleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, saleAttrValueEntity);
                    saleAttrValueEntity.setSkuId(skuId);
                    return saleAttrValueEntity;
                }).collect(Collectors.toList());
                //6.3保存Sku的销售属性信息：pms_product_attr_value
                skuSaleAttrValueService.saveBatch(attrValueEntities);

                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku,skuReductionTo);
                List<MemberPrice> memberPrice = sku.getMemberPrice();
                skuReductionTo.setMemberPrice(memberPrice);
                skuReductionTo.setSkuId(skuId);
                if(skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    //6.4保存Sku的优惠、满减等信息：gulimall_sms -> sms_sku_ladder\sms_sku_full_reduction\sms_member_price
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r1.getCode() != 0){
                        log.error("远程保存Sku的优惠、满减与会员价信息失败");
                    }
                }

            }
        }

    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        this.baseMapper.insert(infoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w ->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }
        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {

        //List<SkuEsModel> upProducts = new ArrayList<>();
        //组装我们需要的数据
        //SkuEsModel esModel = new SkuEsModel();
        //1.查出当前spuId对应的所有sku信息[还包含品牌的名字等]
        List<SkuInfoEntity> skus = skuInfoService.getSkuBySpuId(spuId);
        //远程调用getSkusHasStock需要用到的所有skuId集合
        /**
         *         List<Object> skuIds = skus.stream().map(sku -> {
         *             return sku.getSkuId();
         *         }).collect(Collectors.toList());
         */
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        // 1、发送远程请求，库存查询是否有库存 ==》stockMap标识
        Map<Long, Boolean> stockMap = null;
        try {
            R r = wareFeignService.getSkusHasStock(skuIds);
            //
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {
            };
            stockMap = r.getData(typeReference).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        }catch (Exception e){
            log.error("库存服务查询异常：原因{}",e);
        }

        //2.封装每个sku的信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);
            //skuPrice  skuImg
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            //hasStock  hotScore --> 1、发送远程请求，库存查询是否有库存
            //如果当前远程调用的数据有问题，则默认为有库存
            //TODO 没有库存是否上架的验证规则
            if(finalStockMap == null){
                esModel.setHasStock(true);
            }else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            //TODO 2、热度评分，刚上架的商品假设热度为0
            esModel.setHotScore(0L);

            //brandName brandImg  catalogName
            //3、查询品牌和分类的名字信息
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());

            //TODO 4、查询当前sku的所有可以用来被检索规格属性
            /**
             * private List<Attrs> attrs;
             *      private Long attrId;
             *      private String attrName;
             *      private String attrValue;
             */
            //4.1、查出当前spu中的全部属性  =====》 //sku中的Attrs都是继承spu中的属性的
            List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrListForSpu(spuId);
            List<Long> attrIds = baseAttrs.stream().map(baseAttr -> {

                return baseAttr.getAttrId();
            }).collect(Collectors.toList());

            //查到当前属性中可以被检索的属性ID值 ==》 List<Long>
            List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);

            Set<Long> idSet = new HashSet<>(searchAttrIds);
            //4.2、在指定的所有属性[baseAttrs]集合里面：挑出检索属性[searchAttrIds]
            List<SkuEsModel.Attrs> attrValueEntities = baseAttrs.stream().filter(item -> {
                return idSet.contains(item.getAttrId());
                //4.3在通过Stream流遍历属性:attrId,attrName,attrValue
            }).map(item -> {
                SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
                BeanUtils.copyProperties(item, attrs);
                return attrs;
            }).collect(Collectors.toList());
            //设置检索属性
            esModel.setAttrs(attrValueEntities);


            return esModel;
        }).collect(Collectors.toList());

        //TODO 5、将数据发送给ES进行保存：gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0){
            //远程调用成功
            //TODO 修改上架商品SPU中的发布状态信息 publish_status
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else{
            //远程调用失败（这次上架操作出现问题）
            //TODO 7、重复调用，接口幂等性，重试机制 xxx
            //Feign调用
            /**
             * 1、构造请求数据，将对象转为json
             *  SynchronousMethodHandler类：invoke
             *  RequestTemplate template = this.buildTemplateFromArgs.create(argv);
             *
             * 2、发送请求进行执行(执行成功会解码响应数据)
             *  executeAndDecode(RequestTemplate r,Options o)方法
             *  Request request = this.targetRequest(template);
             *
             * 3.执行请求会有重试机制
             *  invoke方法中如果执行异常，尝试器会尝试继续执行方法远程调用
             *  retryer.continueOrPropagate(e);
             *
             *  1)  如果重试的次数超过了限定的最大次数，抛出异常
             *      continueOrPropagate()
             *      if (this.attempt++ >= this.maxAttempts) {
             *           throw e;
             *      }
             *  当远程调用失败的时候，会使用重试器在catch语句块进行重新调用，只有当超过了最大
             *  重试次数后，continueOrPropagate()方法才会抛出异常，不执行continue，结束调用过程；
             *  while(){
             *     try {
             *          return this.executeAndDecode(template, options);
             *     } catch (RetryableException var9) {
             *         try {
             *              retryer.continueOrPropagate(e);
             *         }catch{throw ex}
             *
             *         continue;
             *  }

             *  扩展，可以写重试器自己的实现，来指定最大的重试次数
             *
             */
        }

    }


}
