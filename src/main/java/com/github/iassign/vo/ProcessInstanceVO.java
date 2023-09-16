package com.github.iassign.vo;

import com.github.iassign.entity.ProcessInstance;
import lombok.Data;

@Data
public class ProcessInstanceVO extends ProcessInstance {
    public String definitionName;
    public String description;
}
