package com.github.iassign.dto;

import com.github.iassign.entity.FormDefinition;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FormDTO extends FormDefinition {
    public Map<String, Object> config;
    public List<Map<String, Object>> children;
}
