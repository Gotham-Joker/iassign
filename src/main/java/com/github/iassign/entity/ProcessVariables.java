package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("t_process_variables")
public class ProcessVariables {
    @TableId
    public String id;
    public String instanceId;
    public String data;

    public ProcessVariables() {

    }

}
