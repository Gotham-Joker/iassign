package com.github.iassign.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.authorization.UserDetails;
import com.github.core.ApiException;
import com.github.core.JsonUtil;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessTask;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.iassign.Constants.MAIL_PATTERN;


/**
 * 邮件提醒，异步
 */
@Slf4j
@Async
@Service
public class ProcessMailService {
    @Value("${iassign.web-url}")
    private String webUrl;
    @Value("${upload.path:/tmp}")
    private String uploadPath; // 从这里获取附件
    @Value("${mail.enabled:true}")
    private Boolean enableMail;

    @Value("${spring.mail.username}")
    private String from;
    private final JavaMailSender javaMailSender;

    private final String TAG = "<I_ASSIGN>";

    public ProcessMailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 发送邮件
     *
     * @param to
     * @param cc
     * @param subject
     * @param content
     * @param attachments
     * @throws Exception
     */
    public void send(String subject, String content, Set<String> to, Set<String> cc, List<File> attachments) {
        if (!Boolean.TRUE.equals(enableMail)) {
            return;
        }
        boolean multipart = attachments != null && !attachments.isEmpty();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(javaMailSender.createMimeMessage(), multipart);
            mimeMessageHelper.setFrom(from);
            if (to == null || to.isEmpty()) {
                throw new ApiException(500, "mail to is required");
            }
            mimeMessageHelper.setTo(to.toArray(new String[0]));
            if (cc != null && !cc.isEmpty()) {
                mimeMessageHelper.setCc(cc.toArray(new String[0]));
            }
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setEncodeFilenames(true);
            if (content != null) {
                if (content.startsWith("<!DOCTYPE HTML") || content.startsWith("<!doctype html")) {
                    mimeMessageHelper.setText(content, true);
                } else {
                    mimeMessageHelper.setText(content);
                }
            }
            if (attachments != null && !attachments.isEmpty()) {
                for (int i = 0; i < attachments.size(); i++) {
                    File file = attachments.get(i);
                    mimeMessageHelper.addAttachment(file.getName(), file);
                }
            }
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (MessagingException e) {
            log.error("邮件发生异常", e);
        }

    }

    /**
     * 生成路由跳转链接
     *
     * @param instanceId
     * @return
     */
    private String generateRouteUrl(String instanceId) {
        return webUrl + "#/process/process-detail?id=" + instanceId;
    }


    /**
     * 将要发送的审批意见整理成html
     *
     * @param instanceId
     * @param definitionName
     * @param taskName
     * @param auditor
     * @param content
     * @return
     */
    public String convertToHtml(String instanceId, String definitionName,
                                String taskName, String starterName,
                                String auditor, String content) {
        content = "<strong>申请单号: " + instanceId + "，申请人: " + starterName + "，申请类型: " + definitionName + "，审批环节: " + taskName + "</strong>" +
                "<p>审批人: " + auditor + "，<a href=\"" + generateRouteUrl(instanceId) + "\">点击跳转至系统</a><br></p>" +
                "<div class=\"remark-p\">" + content + "</div>";
        StringBuilder sb = new StringBuilder();
        try (InputStream in = ProcessService.class.getClassLoader().getResourceAsStream("mailcontent.html");
             InputStreamReader isr = new InputStreamReader(in);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                if (line.equals("<base>")) {
                    sb.append("<base href=\"").append(webUrl).append("\">");
                }
                if (line.equals("<body>")) {
                    sb.append(content);
                }
            }
        } catch (Exception e) {
            log.error("error", e);
            throw new ApiException(500, "邮件内容处理错误");
        }
        return sb.toString();
    }


    /**
     * 启动流程实例的时候，发送邮件
     *
     * @param instance
     */
    public void sendStartMail(ProcessInstance instance) {
        String emails = instance.emails;
        Set<String> emailSet = Arrays.stream(emails.split(",")).filter(Objects::nonNull)
                .filter(email -> MAIL_PATTERN.matcher(email).find()).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        String subject = instance.starterName + "提交了【" + instance.name + "】";
        String content = "<!doctype html><html><div>" + subject + "，申请单号：" + instance.id +
                "，可登录系统查看: <a href=\"" + generateRouteUrl(instance.id) + "\">跳转链接</a></div><p>以上消息来自" + TAG + "，无需回复</p></html>";
        send(TAG + subject, content, emailSet, null, null);
    }

    /**
     * 发送邮件给指派人，只有被指派的人会收到邮件
     *
     * @param sender 发送人
     * @param dto
     * @param task
     */
    public void sendAssignMail(String sender, ProcessClaimAssignDTO dto, ProcessTask task) {
        String email = dto.email;
        if (email != null && !MAIL_PATTERN.matcher(email).find()) {
            return;
        }
        String remark = dto.remark;
        String subject = sender + "指派了一个任务请您处理";
        String content = "<!doctype html><html><div>【" + sender + "】指派了一条任务给您，申请单号:" + task.instanceId
                + "，可登录系统查看: <a href=\"" + generateRouteUrl(task.instanceId) + "\">跳转链接</a></div><p>" + remark + "</p></html>";
        send(TAG + subject, content,
                Collections.singleton(email), null, null);
    }

    /**
     * 发送同意审批的邮件
     *
     * @param auditor  审批人
     * @param instance
     * @param task
     * @param remark
     */
    @Async
    public void sendApproveMail(UserDetails auditor, ProcessInstance instance, ProcessTask task, String remark) {
        String emails = instance.emails;
        Set<String> emailSet = Arrays.stream(emails.split(",")).filter(Objects::nonNull)
                .filter(email -> MAIL_PATTERN.matcher(email).find()).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        // 当前审批人审批意见(同意)
        String subject = instance.starterName + "【" + instance.name + "】审批意见";
        String content = convertToHtml(instance.id, instance.name, task.name, instance.starterName, auditor.username, remark);
        List<File> attachments = retrieveAttachments(task);
        send(TAG + subject, content, emailSet, null, attachments);
    }

    /**
     * 发送一个邮件通知被退回的环节中负责审批的人或角色：任务被退回
     *
     * @param auditor  审批人(发起退回的人)
     * @param instance
     * @param task
     * @param emailSet
     */
    public void sendBackMail(UserDetails auditor, ProcessInstance instance, ProcessTask task, String remark, Set<String> emailSet) {
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        String subject = instance.starterName + "【" + instance.name + "】被退回";
        String content = convertToHtml(instance.id, instance.name, task.name, instance.starterName, auditor.username, remark);
        List<File> attachments = retrieveAttachments(task);
        send(TAG + subject, content, emailSet, null, attachments);
    }

    /**
     * 发送审批被拒绝的邮件，一般来说，被拒绝只需要申请人收到邮件
     *
     * @param auditor
     * @param instance
     * @param task
     * @param email
     */
    public void sendRejectMail(UserDetails auditor, ProcessInstance instance, ProcessTask task, String remark, String email) {
        if (email != null && !MAIL_PATTERN.matcher(email).find()) {
            return;
        }
        String subject = instance.starterName + "【" + instance.name + "】被拒绝";
        String content = convertToHtml(instance.id, instance.name, task.name, instance.starterName, auditor.username, remark);
        List<File> attachments = retrieveAttachments(task);
        send(TAG + subject, content, Collections.singleton(email), null, attachments);
    }

    /**
     * 发送待审批通知
     *
     * @param instance
     * @param emailSet
     */
    public void sendTodoMail(ProcessInstance instance, Set<String> emailSet) {
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        String subject = instance.starterName + "【" + instance.name + "】待您审批";
        String content = "<!doctype html><html><div>" + subject + "，申请单号:" + instance.id
                + "，可登录系统查看: <a href=\"" + generateRouteUrl(instance.id) + "\">跳转链接</a></div></html>";
        send(TAG + subject, content,
                emailSet, null, null);
    }

    /**
     * 发送流程结束邮件
     *
     * @param instance
     */
    public void sendEndMail(ProcessInstance instance) {
        String emails = instance.emails;
        Set<String> emailSet = Arrays.stream(emails.split(",")).filter(Objects::nonNull)
                .filter(email -> MAIL_PATTERN.matcher(email).find()).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        String content = instance.starterName + "【" + instance.name + "】审批通过";
        send(TAG + content, content + "，申请单号:" + instance.id, emailSet, null, null);
    }

    /**
     * 获取附件
     *
     * @param task
     * @return
     */
    private List<File> retrieveAttachments(ProcessTask task) {
        List<File> attachments = null;
        if (StringUtils.hasText(task.attachments)) {
            attachments = new ArrayList<>();
            ArrayNode arrayNode = JsonUtil.readValue(task.attachments, ArrayNode.class);
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode jsonNode = arrayNode.get(i);
                String fileName = jsonNode.get("name").asText();
                attachments.add(new File(uploadPath + File.separator + fileName));
            }
        }
        return attachments;
    }
}
