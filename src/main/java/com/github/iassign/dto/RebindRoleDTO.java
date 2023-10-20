package com.github.iassign.dto;

import lombok.Data;

import java.util.List;

@Data
public class RebindRoleDTO {
    public String roleId;
    public List<String> delUserIds;
    public List<String> addUserIds;
}
