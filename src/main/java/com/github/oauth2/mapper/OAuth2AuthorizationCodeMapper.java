package com.github.oauth2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.oauth2.model.OAuth2AuthorizationCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface OAuth2AuthorizationCodeMapper extends BaseMapper<OAuth2AuthorizationCode> {
    @Delete("delete from oauth2_authorization_code where user_id=#{userId} and client_id=#{clientId}")
    void deleteByUserIdAndClientId(@Param("userId") String id, @Param("clientId") String clientId);
}
