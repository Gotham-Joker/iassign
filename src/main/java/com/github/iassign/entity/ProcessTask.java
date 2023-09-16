package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.iassign.enums.ProcessTaskStatus;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_process_task")
public class ProcessTask {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String definitionId;
    public String instanceId;
    public String formId;
    public String formInstanceId; // 审批人额外填写的表单实例Id
    public String variableId;  // 临时变量,仅对后续非用户审批环节生效
    public String name; // 任务名称
    public String assignId; // 被指派来完成这个任务的用户ID
    public String handlerId; // 受理人的ID
    public String preHandlerId; // 上一受理人的ID

    public String assignAvatar; // 被指派人头像
    public String assignName; // 被指派人姓名
    public String assignEmail;// 被指派人联系方式
    public String handlerAvatar; // 受理人头像
    public String handlerName; // 受理人姓名
    public String handlerEmail; // 受理人联系方式

    public String incomeId; // DAG选择的路线ID
    public String dagNodeId; // 当前DAG节点ID

    public Boolean userNode; // 是否是用户可以审批的环节(有些环节是不能审批的，是系统自动处理的)
    public String remark; // 备注 审批意见
    public String assignRemark; // 指派给别人的时候，填写指派意见，如"xxx麻烦帮我处理下，谢谢" 我觉得后续可以改成任务允许添加评论？毕竟被指派的人也可能回复
    public String attachments; // 附件列表
    public ProcessTaskStatus status; // 状态
    public Date createTime;  // 开始时间
    public Date assignTime; // 受理时间或指派时间 指派其实就默认受理了之后再转交给别人，所以时间用同一个字段
    public Date updateTime; // 完成时间或退回时间
}
