package com.github.iassign.dto;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.github.iassign.entity.ProcessInstanceIndex;
import lombok.Data;

import java.util.List;

@Data
public class ProcessInstanceIndexDTO extends ProcessInstanceIndex {
    public List<String> highlight; // 高亮语句
    public Boolean isHighlight;

    public ProcessInstanceIndexDTO() {

    }

    public ProcessInstanceIndexDTO(Hit<ProcessInstanceIndex> hit) {
        ProcessInstanceIndex source = hit.source();

    }
}
