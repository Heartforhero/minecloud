package org.feiesos.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_refresh_token")
public class SysRefreshToken {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String token;

    private OffsetDateTime expiresAt;

    private Boolean revoked;
}
