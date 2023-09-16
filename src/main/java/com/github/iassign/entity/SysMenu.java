package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    @NotNull(message = "父级菜单不能为空")
    public String pid;
    public Integer weight; // 菜单权重
    public Boolean allAvailable = false; // 所有人可见
    public String text;
    public String icon;
    public String link;
}
