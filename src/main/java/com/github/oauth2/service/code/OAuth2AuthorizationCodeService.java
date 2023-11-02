package com.github.oauth2.service.code;

import com.github.authorization.exception.OAuth2Exception;
import com.github.oauth2.mapper.OAuth2AuthorizationCodeMapper;
import com.github.oauth2.model.OAuth2AuthorizationCode;
import com.github.oauth2.vo.OAuth2AuthenticationRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class OAuth2AuthorizationCodeService {
    @Autowired
    private OAuth2AuthorizationCodeMapper oAuth2AuthorizationCodeMapper;

    public String createAuthorizationCode(String userId, OAuth2AuthenticationRequest request) {
        // 每次生成code都会使上一个code失效
        oAuth2AuthorizationCodeMapper.deleteByUserIdAndClientId(userId, request.getClientId());
        // 把生成的code和用户的授权信息保存到数据库中
        String code = UUID.randomUUID().toString().replaceAll("-", "");
        OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode();
        BeanUtils.copyProperties(request, authorizationCode);
        authorizationCode.setUserId(userId);
        authorizationCode.setCode(code);
        authorizationCode.setCreateTime(new Date());
        oAuth2AuthorizationCodeMapper.insert(authorizationCode);
        return code;
    }

    public OAuth2AuthorizationCode consumeAuthorizationCode(String code) {
        OAuth2AuthorizationCode authorizationCode = Optional.ofNullable(oAuth2AuthorizationCodeMapper.selectById(code)).orElseThrow(
                () -> new OAuth2Exception(500,"invalid code"));
        oAuth2AuthorizationCodeMapper.deleteById(code);
        //code 5分钟后失效
        if (System.currentTimeMillis() - authorizationCode.getCreateTime().getTime() > 300 * 1000) {
            throw new OAuth2Exception(500,"code expired");
        }
        return authorizationCode;
    }

}
