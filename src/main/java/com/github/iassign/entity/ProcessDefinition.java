package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 流程模型，保存着DAG图的json内容
 */
@Data
@TableName("t_process_definition")
public class ProcessDefinition {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String name;
    public String seqNo; // 序号，有时候记住序号比记住流程名有用，例如0001=>休假申请
    public String icon;
    public String description;
    public Boolean status; // 部署状态 0-未部署 1-已部署
    // 不是数据库字段
    @TableField(exist = false)
    public String dag;
    public String ruId; // 一直保存着最新的运行时dag Id
    public String formId;  // 绑定的表单ID  可以建立搜索
    public String creator;
    public String groupName;
    public String managers; // 管理者id列表
    public Date createTime;
    public Date updateTime;
}
