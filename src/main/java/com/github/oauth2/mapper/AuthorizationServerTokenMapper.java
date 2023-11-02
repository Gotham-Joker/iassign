package com.github.oauth2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.oauth2.model.OAuth2AccessToken;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface AuthorizationServerTokenMapper extends BaseMapper<OAuth2AccessToken> {

//    OAuth2AccessToken findByAccessToken(String accessToken);

//    OAuth2AccessToken findByClientIdAndUserId(String clientId,String userId);

    @Delete("delete from oauth2_access_token where client_id=#{clientId} and user_id=#{userId}")
    void deleteByClientIdAndUserId(@Param("clientId") String clientId, @Param("userId") String userId);

}
