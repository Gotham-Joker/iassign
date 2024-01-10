package com.github.iassign.controller;

import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.dto.ProcessTaskDTO;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.service.ProcessOpinionService;
import com.github.iassign.vo.CheckedListQuery;
import com.github.iassign.vo.ProcessTaskTodoQuery;
import com.github.iassign.service.ProcessService;
import com.github.iassign.service.ProcessTaskService;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/process-task")
public class ProcessTaskController {
    @Autowired
    private ProcessService processService;
    @Autowired
    private ProcessTaskService processTaskService;
    @Autowired
    private ProcessOpinionService processOpinionService;

    /**
     * 认领
     *
     * @param dto
     * @return
     */
    @PostMapping("claim")
    public Result claim(@Validated @RequestBody ProcessClaimAssignDTO dto) {
        UserDetails details = AuthenticationContext.details();
        dto.userId = details.getId();
        dto.username = details.username;
        dto.email = details.email;
        dto.remark = null;
        dto.avatar = details.avatar;
        return processTaskService.claim(dto);
    }

    /**
     * 指派
     *
     * @param dto
     * @return
     */
    @PostMapping("assign")
    public Result assign(@Validated @RequestBody ProcessClaimAssignDTO dto) {
        if (!StringUtils.hasText(dto.userId)) {
            return Result.error("指派时必须选定一个用户");
        }
       /* Authentication authentication = AuthenticationContext.current();
        if (!authentication.isAdmin()) {
            return Result.error("管理员才允许指派");
        }*/
        return processTaskService.assign(dto);
    }

    /**
     * 分页查询
     *
     * @param params
     * @return
     */
    @GetMapping
    public Result pageQuery(@RequestParam Map<String, String> params) {
        return Result.success(processTaskService.pageQuery(params));
    }

    /**
     * 查询待办事项
     */
    @GetMapping("todo-list")
    public Result queryTodoList(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                ProcessTaskTodoQuery processTaskTodoQuery) {
        return Result.success(processTaskService.queryTodoList(page, size, processTaskTodoQuery));
    }

    /**
     * 查询已办事项
     */
    @GetMapping("checked-list")
    public Result checkedList(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              CheckedListQuery query) {
        query.userId = AuthenticationContext.current().getId();
        return Result.success(processOpinionService.queryCheckedList(page, size, query));
    }

    /**
     * 取回
     *
     * @return
     */
    @PostMapping("reclaim")
    public Result reclaim(@RequestParam String taskId) {
        UserDetails userDetails = AuthenticationContext.details();
        return processTaskService.reclaim(userDetails, taskId);
    }

    /**
     * 根据流程实例id查询审批历史
     *
     * @return
     */
    @GetMapping("audit-list")
    public Result auditList(@RequestParam String instanceId) {
        return Result.success(processTaskService.auditListAfter(instanceId, null));
    }

    /**
     * 提交任务(同意、拒绝、回退、回退至发起人)
     *
     * @return
     */
    @PostMapping
    public Result handleTask(@Validated @RequestBody ProcessTaskDTO dto) {
        return Result.success(processService.handleTask(dto));
    }

    /**
     * 判查询任务审批权限以及判断当前用户是否有审批权限
     */
    @GetMapping("auth")
    public Result validateAuthorize(@RequestParam String id) {
        ProcessTask task = processTaskService.selectById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.success(processTaskService.validateAuthorize(AuthenticationContext.current().getId(), task));
    }


    /**
     * 恢复失败的系统节点
     */
    @PostMapping("recover")
    public Result recover(@RequestParam String taskId) {
        return Result.success(processService.recover(taskId));
    }
}
