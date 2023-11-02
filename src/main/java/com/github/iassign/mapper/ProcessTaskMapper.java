package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.vo.ProcessTaskTodoQuery;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.vo.TaskTodoVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessTaskMapper extends BaseMapper<ProcessTask> {

    @Select({"<script>",
            "select t.instance_id,t.id task_id,t.name task_name,i.starter,",
            "u.username starter_name,i.name definition_name,t.form_id, t.status,t.create_time ",
            "from t_process_task t inner join t_process_instance i on t.instance_id=i.id ",
            "inner join sys_user u on u.id = i.starter ",
            "<where> ",
            " <if test='instanceId!=null and instanceId!=\"\"'> and t.instance_id=#{instanceId} </if>",
            " <if test='starter!=null and starter!=\"\"'> and i.starter=#{starter} </if>",
            " <if test='createTime_ge!=null and createTime_ge!=\"\"'> and i.create_time &gt;=#{createTime_ge} </if>",
            " <if test='createTime_le!=null and createTime_le!=\"\"'> and i.create_time &lt;=#{createTime_le} </if>",
            " <if test='definitionName!=null and definitionName!=\"\"'> and i.name like CONCAT('%',#{definitionName},'%') </if>",
            " <if test='referenceIds!=null and !referenceIds.isEmpty()'> ",
            " and exists (select 1 from t_process_task_auth a where a.task_id=t.id and reference_id in ",
            "(<foreach collection='referenceIds' separator=',' item='item'>#{item}</foreach>)) ",
            "</if> ",
            " and t.status in (1,2)",
            "</where> order by t.create_time desc",
            "</script>"})
    List<TaskTodoVO> selectTodoList(ProcessTaskTodoQuery processTaskTodoQuery);


    @Update("update t_process_task set status=7 where instance_id=#{instanceId} and status between 0 and 3")
    void cancelByInstanceId(@Param("instanceId") String instanceId);

}
