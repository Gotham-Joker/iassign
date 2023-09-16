package com.github.iassign.dto;

import com.github.iassign.entity.ProcessInstance;
import lombok.Data;

@Data
public class ProcessInstanceDetailDTO extends ProcessInstance {
    public String formData; // 表单实例
    public String dag; // 流程图
    // 审批历史
//    public List<Task>
}
