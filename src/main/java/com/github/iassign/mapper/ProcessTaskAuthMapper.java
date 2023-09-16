package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.entity.ProcessTaskAuth;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProcessTaskAuthMapper extends BaseMapper<ProcessTaskAuth> {
    @Select("select * from t_process_task_auth where task_id=#{taskId}")
    List<ProcessTaskAuth> selectAuthByTaskId(@Param("taskId") String taskId);

    /**
     * 查找指定的审批任务内的可受理角色，关联用户邮件并整理成set集合
     *
     * @param taskId
     * @return
     */
    @Select({"select email from sys_user u ",
            "where exists(select 1 from sys_user_role ur where u.id=ur.user_id and ",
            "exists(select 1 from t_process_task_auth t where t.task_id=#{taskId} and t.type=1 and t.reference_id=ur.role_id))"})
    Set<String> selectRoleUserMailByTaskId(@Param("taskId") String taskId);

}
