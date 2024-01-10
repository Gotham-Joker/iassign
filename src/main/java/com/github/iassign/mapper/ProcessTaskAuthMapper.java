package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.dto.Tuple;
import com.github.iassign.entity.ProcessTaskAuth;
import org.apache.ibatis.annotations.Delete;
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
    @Select({"select id k,email v from sys_user u ",
            "where exists(select 1 from sys_user_role ur where u.id=ur.user_id and ",
            "exists(select 1 from t_process_task_auth t where t.task_id=#{taskId} and t.type=1 and t.reference_id=ur.role_id))"})
    Set<Tuple<String, String>> selectRoleUserMailByTaskId(@Param("taskId") String taskId);

    @Select("select id k,email v from sys_user u where exists(select 1 from t_process_task_auth a where a.task_id=#{taskId} and a.type=0 and a.reference_id=u.id)")
    Set<Tuple<String, String>> selectUserMailByTaskId(@Param("taskId") String taskId);

    /**
     * 判断指定的用户是否拥有审批权限(包括用户所对应的角色是否可以审批，只要用户有一个角色在审批清单中，那就可以审批)
     *
     * @param taskId
     * @param userId
     * @return 审批授权id
     */
    @Select({"select t.id from t_process_task_auth t where t.task_id=#{taskId} and exists (select 1 from sys_user_role ur where ur.user_id=#{userId} and t.type=1 and ur.role_id=t.reference_id)",
            "union all",
            "select t.id from t_process_task_auth t where t.task_id=#{taskId} and type=0 and t.reference_id=#{userId}"})
    Set<String> selectAuthorize(@Param("taskId") String taskId, @Param("userId") String userId);

    /**
     * 查询指定用户是否已经在授权审批清单中(只看用户ID，并不会去判断用户的角色)
     *
     * @param taskId
     * @param userId
     * @return
     */
    @Select("select reference_id from t_process_task_auth where task_id=#{taskId} and reference_id=#{userId} and type=0")
    String selectAuthorizedUser(@Param("taskId") String taskId, @Param("userId") String userId);

    @Delete("delete from t_process_task_auth where task_id=#{taskId}")
    void deleteByTaskId(@Param("taskId") String taskId);

}
