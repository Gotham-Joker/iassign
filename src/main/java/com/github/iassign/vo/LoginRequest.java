package com.github.iassign.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class LoginRequest {
    @NotNull(message = "请输入用户名")
    public String id;
    @NotBlank(message = "请输入密码")
    public String password;
}
