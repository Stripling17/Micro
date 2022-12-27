package com.xinchen.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.xinchen.common.utils.R;
import com.xinchen.gulimall.ware.feign.MemberFeignService;
import com.xinchen.gulimall.ware.vo.FareRespVo;
import com.xinchen.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.ware.dao.WareInfoDao;
import com.xinchen.gulimall.ware.entity.WareInfoEntity;
import com.xinchen.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<WareInfoEntity>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w ->{
                w.eq("id",key).or().like("name",key)
                        .or().like("address",key)
                        .or().like("areacode",key);
            });
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据用户的收货地址计算运费
     * @return
     * @param addrId
     */
    @Override
    public FareRespVo getFare(Long addrId) {
        FareRespVo fareRespVo = new FareRespVo();
        R r = memberFeignService.addrInfo(addrId);
        if(r.getCode() == 0){
            MemberAddressVo data = r.getData("memberReceiveAddress",new TypeReference<MemberAddressVo>() {
            });
            if(data != null){
                String phone = data.getPhone();
                String substring = phone.substring(phone.length() - 1, phone.length());
                BigDecimal bigDecimal = new BigDecimal(substring);
                fareRespVo.setFare(bigDecimal);
                fareRespVo.setAddress(data);
                return fareRespVo;
            }
        }
        return null;
    }

}
