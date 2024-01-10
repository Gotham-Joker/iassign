package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.dto.CheckedListDTO;
import com.github.iassign.entity.ProcessOpinion;
import com.github.iassign.vo.CheckedListQuery;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProcessOpinionMapper extends BaseMapper<ProcessOpinion> {

    @Select("select * from t_process_opinion where task_id=#{taskId} order by create_time")
    List<ProcessOpinion> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 查询已参与的角色ID
     *
     * @param taskId
     * @param participantRoleIds
     * @return
     */
    @Select({"<script>",
            "select distinct ur.role_id from t_process_opinion t inner join sys_user_role ur on t.task_id=#{taskId}",
            " and t.user_id=ur.user_id where ur.role_id in ",
            "<foreach collection=\"roleIds\" item=\"item\" separator=\",\" open='(' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"})
    Set<String> selectParticipantRoles(@Param("taskId") String taskId, @Param("roleIds") Set<String> participantRoleIds);

    @Select("select * from t_process_opinion where task_id=#{taskId} and user_id=#{userId}")
    ProcessOpinion selectByTaskIdAndUserId(@Param("taskId") String taskId, @Param("userId") String userId);

    @Select({"<script>",
            "select po.instance_id,pi.name instance_name,pi.starter_name,po.task_id,t.name task_name,po.create_time,po.operation ",
            "from t_process_opinion po inner join t_process_instance pi ",
            "on po.user_id=#{userId} and po.instance_id = pi.id inner join t_process_task t on po.task_id = t.id",
            "<where> ",
            "<if test='instanceId!=null and instanceId!=\"\"'>and pi.id=#{instanceId}</if>",
            "<if test='createTimeGe!=null and createTimeGe!=\"\"'>and po.create_time &gt;=#{createTimeGe}</if>",
            "<if test='createTimeLe!=null and createTimeLe!=\"\"'>and po.create_time &lt;=#{createTimeLe}</if>",
            "</where> order by po.create_time desc</script>"})
    List<CheckedListDTO> queryCheckedList(CheckedListQuery query);

    @Delete("delete from t_process_opinion where task_id=#{taskId} and user_id=#{userId}")
    void deleteByTaskIdAndUserId(@Param("taskId") String taskId, @Param("userId") String userId);

}
