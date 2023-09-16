package com.github.iassign.vo;

import lombok.Data;

import java.util.List;

/**
 * 任务详情VO
 */
@Data
public class TaskDetailVO {
    public String id;
    public String instanceId;
    public String definitionId;
    public String dag;
    public String formData;
    public List<String> taskList;
}
