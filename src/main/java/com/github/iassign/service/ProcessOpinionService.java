package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.authorization.UserDetails;
import com.github.core.PageResult;
import com.github.iassign.dto.CheckedListDTO;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.dto.ProcessTaskDTO;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessOpinion;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.enums.TaskOperation;
import com.github.iassign.mapper.ProcessOpinionMapper;
import com.github.iassign.mapper.ProcessTaskAuthMapper;
import com.github.iassign.vo.CheckedListQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProcessOpinionService {
    @Autowired
    private ProcessOpinionMapper processOpinionMapper;
    @Autowired
    private ProcessTaskAuthMapper processTaskAuthMapper;

    /**
     * 提交审批意见
     */
    @Transactional
    public void save() {

    }


    /**
     * 查询审批意见，并根据任务id进行分组
     *
     * @param instanceId
     * @param taskIdGe
     * @return
     */
    public Map<String, List<ProcessOpinion>> selectOpinions(String instanceId, String taskIdGe) {
        return processOpinionMapper.selectList(
                new QueryWrapper<ProcessOpinion>().eq("instance_id", instanceId)
                        .ge(StringUtils.hasText(taskIdGe), "task_id", taskIdGe)
                        .orderByAsc("create_time")).stream().collect(Collectors.groupingBy(ProcessOpinion::getTaskId));
    }

    /**
     * 根据任务id和审批用户id查询审批意见
     *
     * @param taskId
     * @param userId
     * @return
     */
    public ProcessOpinion selectByTaskIdAndUserId(String taskId, String userId) {
        return processOpinionMapper.selectByTaskIdAndUserId(taskId, userId);
    }

    /**
     * 提交审批意见
     *
     * @param userDetails
     * @param instance
     * @param task
     * @param dto
     */
    @Transactional
    public ProcessOpinion submitOpinion(UserDetails userDetails, ProcessInstance instance, ProcessTask task, ProcessTaskDTO dto) {
        ProcessOpinion processOpinion = new ProcessOpinion();
        processOpinion.instanceId = instance.id;
        processOpinion.taskId = task.id;
        processOpinion.userId = userDetails.id;
        processOpinion.username = userDetails.username;
        processOpinion.avatar = userDetails.avatar;
        processOpinion.email = userDetails.email;
        processOpinion.remark = dto.safeRemark();
        processOpinion.attachments = dto.attachments;
        processOpinion.createTime = new Date();
        processOpinion.operation = dto.operation;
        processOpinionMapper.insert(processOpinion);
        return processOpinion;
    }

    public List<ProcessOpinion>  selectByTaskId(String taskId) {
        return processOpinionMapper.selectByTaskId(taskId);
    }

    /**
     * 查询已参与的角色ID
     * @param taskId
     * @param participantRoleIds
     * @return
     */
    public Set<String> selectParticipantRoles(String taskId, Set<String> participantRoleIds) {
        return processOpinionMapper.selectParticipantRoles(taskId, participantRoleIds);
    }

    /**
     * 保存指派意见
     */
    @Transactional
    public void submitAssignOpinion(UserDetails userDetails, ProcessTask task, ProcessClaimAssignDTO dto) {
        // 保存指派人的意见
        ProcessOpinion opinion = new ProcessOpinion();
        opinion.instanceId = task.instanceId; // 关联的流程实例ID
        opinion.taskId = task.id; // 关联的任务ID
        opinion.userId = userDetails.id;
        opinion.username = userDetails.username;
        opinion.avatar = userDetails.avatar;
        opinion.email = userDetails.email;
        opinion.remark = dto.remark; // 审批意见
        opinion.createTime = new Date(); // 创建时间
        opinion.operation = TaskOperation.ASSIGN; // 指派标志位 0-否 1-是
        opinion.assignId = dto.userId; // 被指派人ID
        opinion.assignName = dto.username; // 被指派人姓名
        opinion.assignAvatar = dto.avatar; // 被指派人头像
        opinion.assignMail = dto.email; // 被指派人邮箱
        processOpinionMapper.insert(opinion);
    }

    /**
     * 查询已办事项
     * @param page
     * @param size
     * @param query
     * @return
     */
    public PageResult<CheckedListDTO> queryCheckedList(Integer page, Integer size, CheckedListQuery query) {
        PageHelper.startPage(page, size);
        return PageResult.of(processOpinionMapper.queryCheckedList(query));
    }

    @Transactional
    public void deleteByTaskIdAndUserId(String taskId,String userId) {
        processOpinionMapper.deleteByTaskIdAndUserId(taskId, userId);
    }
}
