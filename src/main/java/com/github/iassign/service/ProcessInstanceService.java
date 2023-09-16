package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.authorization.UserDetails;
import com.github.core.GlobalIdGenerator;
import com.github.core.Result;
import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.enums.ProcessInstanceStatus;
import com.github.iassign.mapper.FormInstanceMapper;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import com.github.iassign.mapper.ProcessDefinitionRuMapper;
import com.github.iassign.mapper.ProcessInstanceMapper;
import com.github.iassign.dto.ProcessInstanceDetailDTO;
import com.github.base.BaseService;
import com.github.core.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ProcessInstanceService {
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;
    @Autowired
    private FormInstanceMapper formInstanceMapper;
    @Autowired
    private ProcessDefinitionRuMapper processDefinitionRuMapper;
    @Autowired
    private GlobalIdGenerator globalIdGenerator;

    public PageResult pageQuery(Map<String, String> params) {
        BaseService.pageHelper(params);
        QueryWrapper<ProcessInstance> wrapper = BaseService.wrapper(params);
        List<ProcessInstance> list = processInstanceMapper.selectList(wrapper);
        return PageResult.of(list);
    }


    /**
     * 查找流程实例详情
     *
     * @param id 流程实例id
     * @return
     */
    public Result<ProcessInstanceDetailDTO> findDetail(String id) {
        ProcessInstanceDetailDTO vo = new ProcessInstanceDetailDTO();
        ProcessInstance instance = processInstanceMapper.selectById(id);
        if (instance == null) {
            return Result.error(404, "数据不存在，此数据可能已被删除");
        }
        BeanUtils.copyProperties(instance, vo);
        vo.dag = processDefinitionRuMapper.selectById(instance.ruId).dag;
        vo.formData = formInstanceMapper.selectById(instance.formInstanceId).data;
        return Result.success(vo);
    }

    public ProcessInstance create(ProcessStartDTO dto, String definitionName, ProcessDefinitionRu definitionRu, UserDetails userDetails) {
        ProcessInstance instance = new ProcessInstance();
        // 插入数据库之前就应该生成这个id，后面要用
        instance.id = globalIdGenerator.nextIdStr();
        instance.definitionId = dto.definitionId;
        instance.name = definitionName;
        instance.ruId = definitionRu.id;
        instance.emails = dto.emails;
        instance.starter = dto.starter;
        instance.starterName = userDetails.username;
        instance.deptId = userDetails.deptId;
        instance.handlerId = instance.starter;
        instance.handlerName = userDetails.username;
        instance.createTime = new Date();
        instance.status = ProcessInstanceStatus.RUNNING;
        return instance;
    }

    @Transactional
    public void save(ProcessInstance instance) {
        processInstanceMapper.insert(instance);
    }

    public ProcessInstance selectById(String instanceId) {
        return processInstanceMapper.selectById(instanceId);
    }

    public void updateById(ProcessInstance instance) {
        processInstanceMapper.updateById(instance);
    }
}
