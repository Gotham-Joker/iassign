package com.github.iassign.service;

import com.github.core.ApiException;
import com.github.iassign.dto.ProcessDeployDTO;
import com.github.iassign.entity.ProcessDefinition;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import com.github.base.BaseService;
import com.github.iassign.vo.ProcessDefinitionDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Service
public class ProcessDefinitionService extends BaseService<ProcessDefinition> {

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessDefinitionRuService processDefinitionRuService;

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
            ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(processDefinition.ruId);
            if (definitionRu == null) {
                throw new ApiException(404, "流程图不存在或可能已被删除" + id);
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
        if (dto.deptIds != null) {
            if (!dto.deptIds.isEmpty()) {
                // 解绑部门和流程定义的关系
                processDefinitionMapper.unbind(dto.id);
                // 重新绑定
                processDefinitionMapper.bind(dto.id, dto.deptIds);
            } else {
                // 解绑部门和流程定义的关系
                processDefinitionMapper.unbind(dto.id);
            }
        }
    }

    /**
     * 查询流程定义可编辑信息
     *
     * @param id
     * @return
     */
    public ProcessDefinitionDetailVO findDefinitionDetail(String id) {
        List<String> deptIds = processDefinitionMapper.selectDeployDepartments(id);
        ProcessDefinition definition = processDefinitionMapper.selectById(id);
        return new ProcessDefinitionDetailVO(definition, deptIds);
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

    @Transactional
    public void deleteAuth(Serializable id) {
        processDefinitionMapper.deleteAuth(id);
    }

    @Transactional
    public void update(ProcessDefinition entity) {
        entity.updateTime = new Date();
        if (StringUtils.hasText(entity.dag)) {
            ProcessDefinitionRu ru = processDefinitionRuService.saveIfAbsent(entity);
            entity.ruId = ru.id;
            updateById(entity);
            // 更新流程图时，找出所有未被引用的流程图，然后把它们删了，只保留最新的一个
            processDefinitionRuService.removeUnused(entity.id, ru.id);
        } else {
            updateById(entity);
        }
    }

    /**
     * 导入流程
     *
     * @param definition
     * @param ru
     */
    @Transactional
    public void importDefinition(ProcessDefinition definition, ProcessDefinitionRu ru) {
        ProcessDefinition oldDef = processDefinitionMapper.selectById(definition.id);
        ProcessDefinitionRu oldRu = processDefinitionRuService.selectById(definition.ruId);
        if (oldDef == null) {
            processDefinitionMapper.insert(definition);
        } else {
            processDefinitionMapper.updateById(definition);
        }
        if (oldRu == null) {
            processDefinitionRuService.insert(ru);
        } else {
            processDefinitionRuService.updateById(ru);
        }
    }
}
