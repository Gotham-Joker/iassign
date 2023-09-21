package com.github.iassign.dto;

import com.github.iassign.entity.ProcessInstanceIndex;
import lombok.Data;

import java.util.List;

@Data
public class ProcessInstanceIndexDTO extends ProcessInstanceIndex {
    public List<String> highlight; // 高亮语句
    public Boolean isHighlight;

    public ProcessInstanceIndexDTO() {

    }

}
