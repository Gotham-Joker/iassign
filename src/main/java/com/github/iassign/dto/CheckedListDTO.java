package com.github.iassign.dto;

import com.github.iassign.enums.TaskOperation;
import lombok.Data;

import java.util.Date;

@Data
public class CheckedListDTO {
    public String instanceId;
    public String instanceName;
    public String starterName;
    public String taskName;
    public Date createTime;
    public TaskOperation operation;
}
