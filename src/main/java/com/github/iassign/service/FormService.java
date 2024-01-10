package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.iassign.dto.FormDTO;
import com.github.authorization.AuthenticationContext;
import com.github.base.BaseService;
import com.github.core.GlobalIdGenerator;
import com.github.core.JsonUtil;
import com.github.iassign.dto.Tuple;
import com.github.iassign.entity.FormDefinition;
import com.github.iassign.entity.FormInstance;
import com.github.iassign.mapper.FormDefinitionMapper;
import com.github.iassign.mapper.FormInstanceMapper;
import com.github.core.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FormService {
    @Autowired
    protected FormDefinitionMapper formDefinitionMapper;
    @Autowired
    protected FormInstanceMapper formInstanceMapper;
    @Autowired
    protected GlobalIdGenerator globalIdGenerator;

    public PageResult<FormDefinition> pageQuery(Map<String, String> params) {
        BaseService.pageHelper(params);
        QueryWrapper<FormDefinition> wrapper = BaseService.wrapper(params);
        List<FormDefinition> list = formDefinitionMapper.selectList(wrapper);
        return PageResult.of(list);
    }

    public void saveDefinition(FormDTO formDTO) {
        // 在后端事先生成id
        formDTO.id = globalIdGenerator.nextIdStr();
        formDTO.definition = JsonUtil.toJson(formDTO);
        formDTO.creator = AuthenticationContext.current().getId();
        formDTO.createTime = new Date();
        formDefinitionMapper.insert(formDTO);
    }

    public void updateDefinition(FormDTO formDTO) {
        FormDefinition formDefinition = formDefinitionMapper.selectById(formDTO.id);
        formDefinition.name = formDTO.name;
        formDefinition.description = formDTO.description;
        formDefinition.definition = JsonUtil.toJson(formDTO);
        formDefinition.updateTime = new Date();
        formDefinitionMapper.updateById(formDefinition);
    }

    public FormInstance saveInstance(String formDefinitionId, Map<String, Object> formData, Integer type) {
        FormInstance formInstance = new FormInstance();
        // 将表单数据转换成变量，其实也就是抽取表单中的field和value
        formInstance.type = type;
        formInstance.formDefinitionId = formDefinitionId;
        formInstance.variables = JsonUtil.toJson(toVariables(formData));
        formInstance.data = JsonUtil.toJson(formData);
        formInstanceMapper.insert(formInstance);
        return formInstance;
    }

    public FormInstance updateInstance(String formInstanceId, Map<String, Object> formData, Integer type) {
        FormInstance formInstance = formInstanceMapper.selectById(formInstanceId);
        // 将表单数据转换成变量，其实也就是抽取表单中的field和value
        formInstance.type = type;
        formInstance.variables = JsonUtil.toJson(toVariables(formData));
        formInstance.data = JsonUtil.toJson(formData);
        formInstanceMapper.updateById(formInstance);
        return formInstance;
    }

    /**
     * 解析form表单里面的变量名称和值
     *
     * @param formData
     * @return
     */

    private Object toVariables(Map<String, Object> formData) {
        Map<String, Object> variables = new HashMap<>();
        List<Object> children = (List<Object>) formData.get("children");
        for (int i = 0; i < children.size(); i++) {
            // 取出控件，例如输入框、下拉框、文本框、上传附件
            Map<String, Object> control = (Map<String, Object>) children.get(i);
            // 取出field
            extractValue(variables, control);
            // 最多只能有一层嵌套(row)
            if (control.get("children") != null) {
                List<Object> rowChildren = (List<Object>) control.get("children");
                for (int j = 0; j < rowChildren.size(); j++) {
                    Map<String, Object> childControl = (Map<String, Object>) rowChildren.get(j);
                    extractValue(variables, childControl);
                }
            }
        }
        return variables;
    }

    private static void extractValue(Map<String, Object> variables, Map<String, Object> control) {
        String field = (String) control.get("field");
        if (field != null) {
            variables.put(field, control.get("value"));
        }
    }

    /**
     * 合并变量，把全局变量和表单变量进行一个合并
     *
     * @param formInstanceId
     * @param globalVariables
     * @return
     */
    public Map<String, Object> mergeVariables(String formInstanceId, Map<String, Object> globalVariables) {
        FormInstance formInstance = formInstanceMapper.selectById(formInstanceId);
        Map<String, Object> formVariables = JsonUtil.readValue(formInstance.variables, Map.class);
        if (formVariables != null && !formVariables.isEmpty()) {
            globalVariables.putAll(formVariables);
        }
        return globalVariables;
    }


    public FormDefinition findDefinitionById(String id) {
        return formDefinitionMapper.selectById(id);
    }

    public void deleteDefinition(String id) {
        formDefinitionMapper.deleteById(id);
    }

    public String findInstanceData(String id) {
        QueryWrapper<FormInstance> queryWrapper = new QueryWrapper<FormInstance>()
                .select("data").eq("id", id);
        FormInstance formInstance = formInstanceMapper.selectOne(queryWrapper);
        return formInstance.data;
    }

    /**
     * 查找指定的form定义中所有的field和标签名，例如field:userId,label:用户ID 以及 field:remark,label:备注
     *
     * @param definitionId
     * @return 返回元组清单，元组的k是field，v是label
     */
    public List<Tuple<String, String>> findLabels(String definitionId) {
        List<Tuple<String, String>> list = new ArrayList<>();
        FormDefinition formDefinition = formDefinitionMapper.selectById(definitionId);
        Map map = JsonUtil.readValue(formDefinition.definition, Map.class);
        List<Object> children = (List<Object>) map.get("children");
        for (int i = 0; i < children.size(); i++) {
            // 取出控件，例如输入框、下拉框、文本框、上传附件
            Map<String, Object> control = (Map<String, Object>) children.get(i);
            // 取出field
            String field = (String) control.get("field");
            if (field != null) {
                Object label = control.get("label");
                label = label == null ? "" : label;
                list.add(new Tuple<>(field, label.toString()));
            }
            // 最多只能有一层嵌套(row)
            if (control.get("children") != null) {
                List<Object> rowChildren = (List<Object>) control.get("children");
                for (int j = 0; j < rowChildren.size(); j++) {
                    Map<String, Object> childControl = (Map<String, Object>) rowChildren.get(j);
                    // 取出field
                    String childField = (String) childControl.get("field");
                    if (childField != null) {
                        Object label = childControl.get("label");
                        label = label == null ? "" : label;
                        list.add(new Tuple<>(childField, label.toString()));
                    }
                }
            }
        }
        // 排个序，避免每次执行，顺序都不一样
        return list.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 查找表单定义
     *
     * @param formId
     * @return
     */
    public FormDefinition selectById(String formId) {
        return formDefinitionMapper.selectById(formId);
    }

    /**
     * 保存表单定义
     *
     * @param definition
     */
    public void save(FormDefinition definition) {
        formDefinitionMapper.insert(definition);
    }

    public FormInstance selectInstance(String formInstanceId) {
        return formInstanceMapper.selectById(formInstanceId);
    }
}
