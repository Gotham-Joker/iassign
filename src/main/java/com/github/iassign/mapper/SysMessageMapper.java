package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.entity.SysMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface SysMessageMapper extends BaseMapper<SysMessage> {
    @Update("update sys_message set status=#{status} where to_user_id=#{toUserId}")
    void updateStatusByToUserId(@Param("status") Integer status, @Param("toUserId") String toUserId);

    @Delete("delete from sys_message where to_user_id=#{toUserId}")
    void deleteAllToUserId(@Param("toUserId") String userId);

}
