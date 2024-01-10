package com.github.oauth2.controller;

import com.github.authorization.*;
import com.github.core.JsonUtil;
import com.github.core.Result;
import com.github.iassign.core.util.PlaceHolderUtils;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.dto.ProcessTaskDTO;
import com.github.iassign.dto.SysUserRoleDTO;
import com.github.iassign.entity.FormDefinition;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.service.*;
import com.github.iassign.vo.CheckedListQuery;
import com.github.iassign.vo.ProcessTaskTodoQuery;
import com.github.oauth2.service.token.AuthorizationServerTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuthApi
 * 专门负责开放授权api
 */
@RestController
@RequestMapping("oauth-api")
public class OAuthController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private AuthorizationServerTokenService authorizationServerTokenService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private ProcessTaskService processTaskService;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private ProcessOpinionService processOpinionService;
    @Autowired
    private FormService formService;

    /**
     * 此处的accessToken是对接了Oauth的服务端的accessToken，详情请见AuthorizationCodeController
     *
     * @return
     */
    @GetMapping("user-info")
    public Result<UserDetails> userInfo() {
        return Result.success(AuthenticationContext.details());
    }


    /**
     * 启动流程实例
     */
    @PostMapping("process")
    public Result startInstance(@Validated @RequestBody ProcessStartDTO dto) throws Exception {
        return Result.success(processService.startInstance(dto).id);
    }

    /**
     * 查找某个角色下面有哪些用户
     */
    @GetMapping("role-users")
    public Result selectByUserRole(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   SysUserRoleDTO dto) {
        return Result.success(sysUserService.selectByUserRole(page, size, dto));
    }

    /**
     * 获取用户的主管
     *
     * @return
     */
    @GetMapping("executives")
    public Result selectExecutives() {
        UserDetails details = AuthenticationContext.details();
        return Result.success(sysUserService.selectExecutives(details.deptId));
    }

    /**
     * 流程实例详情
     */
    @GetMapping("process-instance-detail")
    public Result findDetail(@RequestParam String id) {
        return processInstanceService.findDetail(id);
    }


    //******** 流程任务 ********//
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
    @GetMapping("process-task")
    public Result pageQuery(@RequestParam Map<String, String> params) {
        return Result.success(processTaskService.pageQuery(params));
    }

    /**
     * 查询待办事项
     */
    @GetMapping("process-task/todo-list")
    public Result queryTodoList(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                ProcessTaskTodoQuery processTaskTodoQuery) {
        return Result.success(processTaskService.queryTodoList(page, size, processTaskTodoQuery));
    }

    /**
     * 查询已办事项
     */
    @GetMapping("process-task/checked-list")
    public Result checkedList(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              CheckedListQuery query) {
        query.userId = AuthenticationContext.current().getId();
        return Result.success(processOpinionService.queryCheckedList(page, size, query));
    }

    /**
     * 根据流程实例id查询审批历史
     *
     * @return
     */
    @GetMapping("process-task/audit-list")
    public Result auditList(@RequestParam String instanceId) {
        return Result.success(processTaskService.auditListAfter(instanceId, null));
    }

    /**
     * 提交任务(同意、拒绝、回退)
     *
     * @return
     */
    @PostMapping("process-task")
    public Result handleTask(@Validated @RequestBody ProcessTaskDTO dto) {
        return Result.success(processService.handleTask(dto));
    }

    /**
     * 判查询任务审批权限以及判断当前用户是否有审批权限
     */
    @GetMapping("process-task/auth")
    public Result validateAuthorize(@RequestParam String id) {
        ProcessTask task = processTaskService.selectById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.success(processTaskService.validateAuthorize(AuthenticationContext.current().getId(), task));
    }

    /**
     * 查找表单，并且进行上下文变量替换
     *
     * @return
     */
    @GetMapping("forms/def/context")
    public Result findByIdWithContext(@RequestParam String id) {
        FormDefinition formDefinition = formService.findDefinitionById(id);
        String json = JsonUtil.toJson(formDefinition);
        UserDetails details = AuthenticationContext.details();
        Map<String, Object> context = new HashMap<>();
        context.put("USER_ID", details.id);
        context.put("USERNAME", details.username);
        context.put("DEPT_ID", details.deptId);
        context.put("DEPT_CODE", details.deptCode);
        json = PlaceHolderUtils.replace(json, context);
        FormDefinition definition = JsonUtil.readValue(json, FormDefinition.class);
        return Result.success(definition);
    }

}
