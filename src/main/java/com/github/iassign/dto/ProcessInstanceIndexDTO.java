package com.github.iassign.dto;

import com.github.iassign.entity.ProcessInstanceIndex;
import lombok.Data;

import java.util.List;

@Data
public class ProcessInstanceIndexDTO extends ProcessInstanceIndex {
    public List<String> highlight;
    public Boolean isHighlight;
    public String score; // 分数

    public ProcessInstanceIndexDTO() {

    }

}
