package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 因为要查询，可能放MongoDB会比较好？
 */
@Data
@TableName("t_form_instance")
public class FormInstance {
    public String id;
    public String formDefinitionId;
    public String data;
    // 从data中解析出来的键值对，保存在这里
    public String variables;
    public Integer type; // 0-流程实例表单 1-任务实例表单
    public Date createTime;
    public Date updateTime;
}
