package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会签记录表
 */
@Data
@TableName("t_process_countersign")
public class ProcessCountersign {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String taskId;// 任务ID
    public String referenceId; // 参考ID
    public String remark;
    public String attachments;
    public Date createTime;
    public String formInstanceId;
    public String userId;
    public String username;
    public String userAvatar;
    public String userEmail;
}
