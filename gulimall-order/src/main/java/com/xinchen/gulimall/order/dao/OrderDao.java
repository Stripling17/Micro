package com.xinchen.gulimall.order.dao;

import com.xinchen.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:37:56
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
