package com.aiwechat.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应 users 表
 */
@Data
@TableName("users")
public class User {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信OpenID */
    private String openId;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像URL */
    private String avatarUrl;

    /** 用户状态：active/banned */
    private String status;

    /** 用户角色（非数据库字段） */
    @TableField(exist = false)
    private String role;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;
}
