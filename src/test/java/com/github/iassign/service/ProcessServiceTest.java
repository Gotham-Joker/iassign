package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.SpringApplicationTests;
import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.dto.ProcessTaskDTO;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.enums.TaskOperation;
import com.github.iassign.mapper.ProcessTaskMapper;
import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;


class ProcessServiceTest extends SpringApplicationTests {
    @Autowired
    ProcessService processService;
    @Autowired
    ProcessTaskMapper processTaskMapper;
    @Autowired
    ProcessTaskService processTaskService;
    @Mock
    Authentication authentication;

    @Test
    void startInstance() throws Exception {
        Mockito.when(authentication.getId()).thenReturn("Judith");
        AuthenticationContext.setAuthentication(authentication);
        Map<String, Object> variables = new HashMap<>();
        variables.put("approval", "是");
        ProcessStartDTO dto = new ProcessStartDTO();
        dto.definitionId = "1630481860491280386";
        dto.variables = variables;
        processService.startInstance(dto);
        String[] str = new String[]{"Alex", "Oscar", "Thomas", "Carl"};
        int i = 0;
        while (i < 4) {
            ProcessTask task = processTaskMapper.selectList(new QueryWrapper<ProcessTask>().orderByDesc("id")).get(0);
//            processTaskService.claim(task.id, str[i], str[i], str[i]);
            // 处理任务
            ProcessTaskDTO taskDTO = new ProcessTaskDTO();
            taskDTO.taskId = task.id;
            taskDTO.operation = TaskOperation.APPROVE;
            taskDTO.setRemark("同意!");
            processService.handleTask(taskDTO);
            i++;
        }
    }

    @Test
    void testStartInstance() {
    }

    @Test
    void handleTask() throws Exception {
//        processTaskService.claim("1630471052070891522", "admin", "admin", "2428");

        // 处理任务
        ProcessTaskDTO dto = new ProcessTaskDTO();
        dto.taskId = "1630471052070891522";
        dto.operation = TaskOperation.APPROVE;
        dto.setRemark("同意!");
        /*Map<String, Object> variables = new HashMap<>();
        variables.put("approval", "否");
        if (!variables.isEmpty()) {
            dto.variables = variables;
        }*/
        processService.handleTask(dto);
    }

    @Test
    void testBack() throws Exception { // 退回到指定环节
        // 先受理任务
//        processTaskService.claim("1630463418354434050", "admin", "admin", "2428");

        // 回退任务
        ProcessTaskDTO dto = new ProcessTaskDTO();
        dto.taskId = "1630463418354434050";
        // 回退到哪个审批环节，只能回退到用户环节，回退到系统处理、网关之类的环节是不允许的。
        dto.backwardTaskId = "1630461549469454337";
        dto.operation = TaskOperation.BACK;
        dto.setRemark("不合格，需要退回重写!");
        processService.handleTask(dto);
    }
}