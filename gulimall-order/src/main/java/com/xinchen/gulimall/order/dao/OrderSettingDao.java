package com.xinchen.gulimall.order.dao;

import com.xinchen.gulimall.order.entity.OrderSettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单配置信息
 * 
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:37:56
 */
@Mapper
public interface OrderSettingDao extends BaseMapper<OrderSettingEntity> {
	
}
