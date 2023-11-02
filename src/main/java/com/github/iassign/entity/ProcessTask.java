package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.iassign.enums.ProcessTaskStatus;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("t_process_task")
public class ProcessTask {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String definitionId;
    public String instanceId;
    public String formId;
    public String variableId;  // 临时变量,仅对后续非用户审批环节生效
    public String formInstanceId; // 提交的表单ID 会签的时候，共享同一个表单，即：多个人填写同一个表单
    public String name; // 任务名称
    public String handlerId; // 受理人的ID
    public String preHandlerId; // 上一受理人的ID
    public String incomeId; // 进入当前DAG节点的路线ID
    public String dagNodeId; // 当前DAG节点ID

    public Boolean countersign; // 是否是会签节点 0-否 1-是
    public Boolean userNode; // 是否是用户可以审批的环节(有些环节是不能审批的，是系统自动处理的)
    public ProcessTaskStatus status; // 状态
    public Date createTime;  // 开始时间
    public Date updateTime; // 完成时间或退回时间

    // 审批意见
    @TableField(exist = false)
    public List<ProcessOpinion> opinions;
}
