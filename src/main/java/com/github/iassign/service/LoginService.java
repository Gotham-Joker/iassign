package com.github.iassign.service;

import com.github.authorization.UserDetails;
import com.github.core.Result;
import com.github.iassign.entity.SysUser;
import com.github.iassign.vo.LoginRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    @Autowired
    AccessTokenService accessTokenService;
    @Autowired
    SysUserService sysUserService;

    public Result login(LoginRequest loginRequest) {
        SysUser sysUser = sysUserService.selectById(loginRequest.id);
        if (sysUser == null) {
            return Result.error(401, "用户不存在");
        }
        // TODO 校验密码
        UserDetails userDetails = new UserDetails();
        BeanUtils.copyProperties(sysUser, userDetails);
        return Result.success(accessTokenService.createAccessToken(userDetails));
    }

    public void logout() {
        accessTokenService.eraseAccessToken();
    }
}
