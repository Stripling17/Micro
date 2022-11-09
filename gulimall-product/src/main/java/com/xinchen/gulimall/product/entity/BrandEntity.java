package com.xinchen.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xinchen.common.validator.group.AddGroup;
import com.xinchen.common.validator.group.ListValue;
import com.xinchen.common.validator.group.UpdateGroup;
import com.xinchen.common.validator.group.UpdateStatusGroup;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * 品牌
 * 
 * @author Li Chonggao
 * @email lichonggao@qq.com
 * @date 2022-07-05 09:43:38
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 品牌id
	 */
	@NotNull(message = "修改 必须指定品牌ID" , groups = {UpdateGroup.class , UpdateStatusGroup.class})
	@Null(message = "新增不能指定ID" , groups = AddGroup.class)
	@TableId
	private Long brandId;
	/**
	 * 品牌名
	 */
	//不为空或者不为空字符串
	//@NotEmpty
	//至少包含一个非空字符
	@NotBlank(message = "品牌名必须提交" , groups = {AddGroup.class,UpdateGroup.class})
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotEmpty(message = "新增时品牌logo地址不能为空" , groups = AddGroup.class)
	@URL(message = "logo必须是一个合法的url地址" , groups = {AddGroup.class,UpdateGroup.class})
	private String logo;
	/**
	 * 介绍
	 */
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
	//@Pattern()
	@NotNull(message = "添加修改或修改状态时，不能为空",groups = {AddGroup.class , UpdateStatusGroup.class})
	@ListValue(vals={0,1} , groups = {AddGroup.class , UpdateStatusGroup.class})
	private Integer showStatus;
	/**
	 * 检索首字母
	 */
	@NotEmpty(message = "新增时字段不能为空，修改时字段可以为空，不更新数据库原来的字段数据" , groups = AddGroup.class)
	@Pattern(regexp = "^[a-zA-Z]$" , message = "检索首字母必须是一个字母" , groups = {AddGroup.class,UpdateGroup.class})
	private String firstLetter;
	/**
	 * 排序
	 */
	@NotNull(groups = AddGroup.class)
	@Min(value = 0 , message = "排序必须大于等于0" , groups = {AddGroup.class,UpdateGroup.class})
	private Integer sort;

}
