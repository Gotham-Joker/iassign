package com.github.iassign.service;

import com.github.core.ApiException;
import com.github.iassign.dto.ProcessDeployDTO;
import com.github.iassign.entity.ProcessDefinition;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import com.github.base.BaseService;
import com.github.iassign.mapper.ProcessDefinitionRuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;

@Service
public class ProcessDefinitionService extends BaseService<ProcessDefinition> {

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessDefinitionRuMapper processDefinitionRuMapper;

    /**
     * 查找流程定义详情
     *
     * @param id
     * @return
     */
    @Override
    public ProcessDefinition selectById(Serializable id) {
        ProcessDefinition processDefinition = super.selectById(id);
        if (StringUtils.hasText(processDefinition.ruId)) {
            ProcessDefinitionRu definitionRu = processDefinitionRuMapper.selectById(processDefinition.ruId);
            if (definitionRu == null) {
                throw new ApiException(404, "流程图不存在或可能已被删除");
            }
            processDefinition.dag = definitionRu.dag;
        }
        return processDefinition;
    }

    @Transactional
    @Override
    public void delete(Serializable id) {
        super.delete(id);
        // 解绑权限
        processDefinitionMapper.unbind((String) id);
    }

    /**
     * 部署流程定义
     *
     * @param dto
     */
    @Transactional
    public void deploy(ProcessDeployDTO dto) {
        ProcessDefinition definition = new ProcessDefinition();
        definition.id = dto.id;
        definition.status = dto.status;
        baseMapper.updateById(definition);
        if (dto.deptIds != null && !dto.deptIds.isEmpty()) {
            // 解绑部门和流程定义的关系
            processDefinitionMapper.unbind(definition.id);
            // 重新绑定
            processDefinitionMapper.bind(definition.id, dto.deptIds);
        }
    }

    /**
     * 查询流程定义的权限
     *
     * @param id
     * @return
     */
    public List<String> findPermission(String id) {
        return processDefinitionMapper.selectPermission(id);
    }

    /**
     * 查找用户能看到的流程（前提是流程部署在该用户的部门下或者流程是ALL部门可见的）
     *
     * @param keyword 关键字查询
     * @param deptIds 用户的部门，ALL的话表示所有部门可见的流程
     * @return
     */
    public List<ProcessDefinition> selectUsersDefinitions(String keyword, List<String> deptIds) {
        return processDefinitionMapper.selectUsersDefinitions(keyword, deptIds);
    }
}
