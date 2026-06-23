package com.aiwechat.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String openId;
    private String nickname;
    private String avatarUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
