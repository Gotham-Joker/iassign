package com.github.iassign.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SysUserRoleDTO {
    public String id;
    public String username;
    public String email;
    public String roleId;
    public String roleIdIn;
    public String roleIdLike;
    public String roleIdLikeLeft;
    public String roleIdLikeRight;

    public Set<String> getRoleIds() {
        if (StringUtils.hasText(roleIdIn)) {
            return Arrays.stream(roleIdIn.split(",")).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
