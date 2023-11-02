package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.iassign.enums.TaskOperation;
import lombok.Data;

import java.util.Date;

/**
 * 审批意见表
 */
@Data
@TableName("t_process_opinion")
public class ProcessOpinion {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String instanceId; // 关联的流程实例ID
    public String taskId; // 关联的任务ID
    public String userId;
    public String username;
    public String avatar;
    public String email;
    public String attachments;
    public String remark; // 审批意见
    public Date createTime; // 创建时间
    public TaskOperation operation; // 操作标志位 0-否决 1-同意 2-退回 3-指派
    public String assignId; // 被指派人ID
    public String assignName; // 被指派人姓名
    public String assignAvatar; // 被指派人头像
    public String assignMail; // 被指派人邮箱
}
