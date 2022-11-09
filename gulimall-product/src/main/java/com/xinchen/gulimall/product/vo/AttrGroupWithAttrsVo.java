package com.xinchen.gulimall.product.vo;

import com.xinchen.gulimall.product.entity.AttrEntity;
import com.xinchen.gulimall.product.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

@Data
public class AttrGroupWithAttrsVo extends AttrGroupEntity {
    private List<AttrEntity> Attrs;
}
