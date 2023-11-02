package com.github.iassign.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.iassign.entity.SysMessage;
import com.github.iassign.mapper.SysMessageMapper;
import com.github.iassign.entity.SysUser;
import com.github.iassign.mapper.SysUserMapper;
import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.base.BaseService;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessTask;
import io.reactivex.rxjava3.core.Observable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 站内信服务，会消费来自kafka的sysMessageService-in-0通道的topic数据(在application.yml中配置destination)进行消费
 */
@Slf4j
@Service
public class SysMessageService extends BaseService<SysMessage> implements Consumer<String> {
    @Autowired
    private SysMessageMapper sysMessageMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private AccessTokenService accessTokenService;
//    @Autowired
//    private StreamBridge streamBridge;

    private final ConcurrentHashMap<String, SseEmitter> sseMap = new ConcurrentHashMap<>();


    public SseEmitter connect(String token) {
//        log.info("sse connect:{}", token);
        Authentication authentication = accessTokenService.parse(token);
        final String userId = authentication.getId();
        SseEmitter sseEmitter = sseMap.get(userId);
        if (sseEmitter == null) {
            // 注册一个sseEmitter，不超时(超时后js客户端会重连)
            sseEmitter = new SseEmitter(-1L);
            sseEmitter.onError(e -> {
                log.error("sse error:{}", userId);
                sseMap.remove(userId);
            });
            sseEmitter.onCompletion(() -> {
//                log.info("sse complete:{}", userId);
                sseMap.remove(userId);
            });
            sseEmitter.onTimeout(() -> {
                log.info("sse timeout:{}", userId);
                sseMap.remove(userId);
            });
            sseMap.put(userId, sseEmitter);
        }
        return sseEmitter;
    }

    /**
     * 发送站内信
     *
     * @param sysMessages
     * @param userDetails
     */
    @Async
    @Transactional
    public void send(List<SysMessage> sysMessages, UserDetails userDetails) {
        Set<String> toUserIds = new HashSet<>();
        for (int i = 0; i < sysMessages.size(); i++) {
            SysMessage sysMessage = sysMessages.get(i);
            final String userId = sysMessage.toUserId;
            toUserIds.add(userId);
            if (StringUtils.hasText(userId)) {
                sysMessage.status = 0;
                sysMessage.fromUserId = userDetails.id;
                sysMessage.fromUserAvatar = userDetails.avatar;
                sysMessage.fromUsername = userDetails.username;
                sysMessage.createTime = new Date();
                sysMessage.type = sysMessage.type == null ? 1 : sysMessage.type;
                save(sysMessage);
            }
        }
        // 让kafka通知所有服务器实例，哪台服务器持有客户端的连接，那么该服务器就负责发送站内信

        //            streamBridge.send("aod_msg.topic", toUserId)
        toUserIds.forEach(this);
    }

    /**
     * 标记为逻辑删除
     *
     * @param id 如果不传，所有自己的消息标记为已读
     */
    @Transactional
    public void markAsRead(String id) {
        if (StringUtils.hasText(id)) {
            // 暂时先真删除
            sysMessageMapper.deleteById(id);
           /* SysMessage sysMessage = sysMessageMapper.selectById(id);
            if (sysMessage != null) {
                sysMessage.status = 2;
                updateById(sysMessage);
            }*/
        } else {
            String userId = AuthenticationContext.current().getId();
            // 暂时先真删除
            sysMessageMapper.deleteAllToUserId(userId);
//            sysMessageMapper.updateStatusByToUserId(2, userId);
        }
    }

    /**
     * 收到来自kafka的消息，给客户发送一条站内信
     *
     * @param toUserId 站内信发送给谁
     */
    @Override
    public void accept(String toUserId) {
        SseEmitter sseEmitter = sseMap.get(toUserId);
        if (sseEmitter != null) {
            try {
                sseEmitter.send("");
            } catch (IOException e) {
                log.error("站内信发送失败", e);
            }
        }
    }

    /**
     * 真正通知浏览器关闭连接并且不再重连
     *
     * @param userId
     */
    public void closeSse(String userId) {
        SseEmitter sseEmitter = sseMap.get(userId);
        if (sseEmitter != null) {
            try {
                Observable.just(1).flatMap(t -> {
                    sseEmitter.send("close");
                    return Observable.timer(1000, TimeUnit.SECONDS);
                }).subscribe(next -> sseEmitter.complete());
            } catch (Exception e) {
                log.error("关闭失败");
            }
        }
    }

    /**
     * 发送待办通知
     */
    @Async
    public void sendAsyncTodoMsg(ProcessInstance instance, ProcessTask task, Set<String> mailSet) {
        if (CollectionUtils.isEmpty(mailSet)) {
            return;
        }
        List<SysUser> sysUsers = sysUserMapper.selectList(new QueryWrapper<SysUser>().in("email", mailSet));
        sysUsers.forEach(sysUser -> {
            SysMessage sysMessage = new SysMessage();
            sysMessage.toUserId = sysUser.id;
            sysMessage.fromUserId = "";
            sysMessage.fromUsername = "系统";
            sysMessage.fromUserAvatar = "/assets/cat.jpg"; // 系统头像
            sysMessage.subject = "待办事项";
            sysMessage.content = "您有一个待办事项：" + instance.starterName + "[" + instance.name + "]，审批环节：" + task.name;
            sysMessage.status = 0;
            sysMessage.createTime = new Date();
            sysMessage.link = instance.id;
            save(sysMessage);
            accept(sysMessage.toUserId);
        });
    }

    /**
     * 发送指派消息
     *
     * @param dto
     * @param task
     * @param userDetails 当前登录用户信息
     */
    @Async
    public void sendAssignMsg(ProcessClaimAssignDTO dto, ProcessTask task, UserDetails userDetails) {
        SysMessage sysMessage = new SysMessage();
        sysMessage.toUserId = dto.userId;
        sysMessage.fromUserId = userDetails.id;
        sysMessage.fromUsername = userDetails.username;
        sysMessage.fromUserAvatar = userDetails.avatar;
        sysMessage.subject = "任务指派";
        sysMessage.content = "您收到了一个指派任务[" + task.name + "] " + dto.remark;
        sysMessage.status = 0;
        sysMessage.createTime = new Date();
        sysMessage.link = task.instanceId;
        save(sysMessage);
        accept(sysMessage.toUserId);
    }

    /**
     * 发送成功的站内信
     *
     * @param instance
     */
    @Async
    public void sendSuccessMsg(ProcessInstance instance) {
        SysMessage sysMessage = new SysMessage();
        sysMessage.toUserId = instance.starter;
        sysMessage.fromUserId = "";
        sysMessage.fromUsername = "系统消息";
        sysMessage.fromUserAvatar = "/assets/cat.jpg";
        sysMessage.subject = "审批成功";
        sysMessage.content = "恭喜，您的[" + instance.name + "]已全部审批通过";
        sysMessage.status = 0;
        sysMessage.createTime = new Date();
        sysMessage.link = instance.id;
        save(sysMessage);
        accept(sysMessage.toUserId);
    }

    @Async
    public void sendRejectMsg(ProcessInstance instance, ProcessTask task, UserDetails details) {
        SysMessage sysMessage = new SysMessage();
        sysMessage.toUserId = instance.starter;
        sysMessage.fromUserId = details.id;
        sysMessage.fromUsername = details.username;
        sysMessage.fromUserAvatar = details.avatar;
        sysMessage.subject = "审批失败";
        sysMessage.content = "抱歉，您的[" + instance.name + "]在<" + task.name + ">环节被拒绝，审批不通过";
        sysMessage.status = 0;
        sysMessage.createTime = new Date();
        sysMessage.link = instance.id;
        save(sysMessage);
        accept(sysMessage.toUserId);
    }
}
