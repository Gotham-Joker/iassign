package com.github.iassign.service;

import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.dto.Tuple;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.entity.ProcessTaskAuth;
import com.github.iassign.mapper.ProcessTaskAuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ProcessTaskAuthService {
    @Autowired
    private ProcessTaskAuthMapper processTaskAuthMapper;

    /**
     * 判断指定的用户是否拥有审批权限(包括用户所对应的角色是否可以审批，只要用户有一个角色在审批清单中，那就可以审批)
     *
     * @param taskId
     * @param userId
     * @return
     */
    public Set<String> selectAuthorize(String taskId, String userId) {
        return processTaskAuthMapper.selectAuthorize(taskId, userId);
    }

    /**
     * 查询指定用户是否已经在授权审批清单中(只看用户ID，并不会去判断用户的角色)
     *
     * @param taskId
     * @param userId
     * @return
     */
    public String selectAuthorizedUser(String taskId, String userId) {
        return processTaskAuthMapper.selectAuthorizedUser(taskId, userId);
    }

    /**
     * 添加授权
     *
     * @param dto
     * @param task
     */
    public void addAuthorize(ProcessClaimAssignDTO dto, ProcessTask task) {
        ProcessTaskAuth processTaskAuth = new ProcessTaskAuth();
        processTaskAuth.type = 0;
        processTaskAuth.taskId = task.id;
        processTaskAuth.referenceId = dto.userId;
        processTaskAuth.name = dto.username;
        processTaskAuth.avatar = dto.avatar;
        processTaskAuthMapper.insert(processTaskAuth);
    }

    public void save(ProcessTaskAuth auth) {
        processTaskAuthMapper.insert(auth);
    }

    /**
     * 按角色查找所有可审批人的邮件 k:id v:email,会忽略type=0(即人员)的授权
     * @param id
     * @return
     */
    public  Set<Tuple<String,String>> selectRoleUserMailByTaskId(String id) {
        return processTaskAuthMapper.selectRoleUserMailByTaskId(id);
    }

    /**
     * 按用户查找所有可审批人的邮件 k:id v:email,会忽略type=1(即角色)的授权
     * @param taskId
     * @return
     */
    public Set<Tuple<String, String>> selectUserMailByTaskId(String taskId) {
        return processTaskAuthMapper.selectUserMailByTaskId(taskId);
    }

    public List<ProcessTaskAuth> selectAuthByTaskId(String taskId) {
        return processTaskAuthMapper.selectAuthByTaskId(taskId);
    }

    @Transactional
    public void deleteByTaskId(String taskId) {
        processTaskAuthMapper.deleteByTaskId(taskId);
    }


}
