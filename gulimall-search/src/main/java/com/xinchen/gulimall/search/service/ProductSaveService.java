package com.xinchen.gulimall.search.service;

import com.xinchen.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {
    //商品上架
    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
