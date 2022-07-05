package com.xinchen.gulimall.member.dao;

import com.xinchen.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:31:04
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
