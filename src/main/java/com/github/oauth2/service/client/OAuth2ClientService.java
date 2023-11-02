package com.github.oauth2.service.client;

import cn.hutool.crypto.symmetric.DES;
import com.github.authorization.exception.NoSuchClientException;
import com.github.core.JsonUtil;
import com.github.oauth2.Constants;
import com.github.oauth2.mapper.Oauth2ClientMapper;
import com.github.oauth2.model.OAuth2Client;
import com.github.oauth2.vo.OAuth2AuthenticationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OAuth2ClientService {
    @Autowired
    private Oauth2ClientMapper oauth2ClientMapper;

    public OAuth2Client checkRequest(String clientId) throws NoSuchClientException {
        Optional<OAuth2Client> optional = Optional.ofNullable(oauth2ClientMapper.selectById(clientId));
        return optional.orElseThrow(() -> new NoSuchClientException("no such client"));
    }

    public String createState(OAuth2AuthenticationRequest request) {
        DES des = new DES(Constants.DES_KEY);
        // 校验通过，返回前端一段密文
        return des.encryptHex(JsonUtil.toJson(request));
    }

    public OAuth2AuthenticationRequest parseState(String state) {
        DES des = new DES(Constants.DES_KEY);
        return JsonUtil.readValue(des.decryptStr(state), OAuth2AuthenticationRequest.class);
    }
}
