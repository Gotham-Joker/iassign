package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_form_definition")
public class FormDefinition {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String name;
    public String description;
    public String definition; // 表单定义json
    public String creator;
    public Date createTime;
    public Date updateTime;
}
