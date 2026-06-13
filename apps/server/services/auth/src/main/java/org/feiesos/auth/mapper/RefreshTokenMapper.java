package org.feiesos.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.feiesos.auth.entity.SysRefreshToken;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<SysRefreshToken> {

    @Select("SELECT * FROM sys_refresh_token WHERE token = #{token} AND revoked = false")
    SysRefreshToken findByToken(String token);
}
