package com.xinchen.gulimall.coupon.dao;

import com.xinchen.gulimall.coupon.entity.CouponHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取历史记录
 * 
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 10:16:46
 */
@Mapper
public interface CouponHistoryDao extends BaseMapper<CouponHistoryEntity> {
	
}
