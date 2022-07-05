package com.xinchen.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.gulimall.member.entity.MemberCollectSubjectEntity;

import java.util.Map;

/**
 * 会员收藏的专题活动
 *
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:31:04
 */
public interface MemberCollectSubjectService extends IService<MemberCollectSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

