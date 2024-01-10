package com.github.iassign.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.core.ApiException;
import com.github.core.JsonUtil;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessOpinion;
import com.github.iassign.entity.ProcessTask;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    @Value("${mail.enabled:true}")
    private Boolean enableMail;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private MailProperties mailProperties;

    private final ObjectMapper objectMapper;

    private final String TAG = "<I_ASSIGN>";

    public ProcessMailService() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 发送邮件通知
     *
     * @param subject     邮件主题
     * @param content     邮件正文
     * @param receivers   邮件接收人
     * @param receiversCc 邮件抄送人
     * @param attachments 附件
     */
    public void send(String subject, String content,
                     Collection<String> receivers, Collection<String> receiversCc,
                     List<File> attachments) {
        try {
            boolean multipart = attachments != null && !attachments.isEmpty();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(javaMailSender.createMimeMessage(), multipart);
            mimeMessageHelper.setEncodeFilenames(true);
            mimeMessageHelper.setFrom(mailProperties.getUsername());
            mimeMessageHelper.setSubject(subject);
            if (receivers == null || receivers.isEmpty()) {
                throw new RuntimeException("mail receivers is required");
            }
            mimeMessageHelper.setTo(receivers.toArray(new String[0]));
            if (receiversCc != null && !receiversCc.isEmpty()) {
                mimeMessageHelper.setCc(receiversCc.toArray(new String[0]));
            }

            // 添加附件区
            if (multipart) {
                for (int i = 0; i < attachments.size(); i++) {
                    File file = attachments.get(i);
                    FileDataSource dataSource = new FileDataSource(file);
                    dataSource.setFileTypeMap(mimeMessageHelper.getFileTypeMap());
                    try {
                        MimeBodyPart mimeBodyPart = new MimeBodyPart();
                        mimeBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
                        // 解决附件名不支持中文的问题
                        mimeBodyPart.setFileName(MimeUtility.encodeText(file.getName(), "UTF-8", "B"));
                        mimeBodyPart.setDataHandler(new DataHandler(dataSource));
                        mimeMessageHelper.getRootMimeMultipart().addBodyPart(mimeBodyPart);
                    } catch (UnsupportedEncodingException ex) {
                        throw new MessagingException("Failed to encode attachment filename", ex);
                    }
                }
            }

            // 添加文本区
            if (content == null) {
                content = "";
            }
            if (content.startsWith("<!DOCTYPE HTML") || content.startsWith("<!doctype html")) {
                mimeMessageHelper.setText(content, true);
            } else {
                mimeMessageHelper.setText(content);
            }
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (Exception e) {
            log.error("邮件发送失败：", e);
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
                                String auditor, String content, String attachmentPart) {
        content = "<strong>申请单号: " + instanceId + "，申请人: " + starterName + "，申请类型: " + definitionName + "，审批环节: " + taskName + "</strong>" +
                "<p>审批人: " + auditor + "，<a href=\"" + generateRouteUrl(instanceId)
                + "\">点击跳转至系统(提示：请使用edge浏览器打开，若不幸打开了ie，请复制ie浏览器地址栏，粘贴至edge的地址栏)</a><br></p>" +
                attachmentPart +
                "<div class=\"remark-p\">" + content + "</div>";
        StringBuilder sb = new StringBuilder();
        try (InputStream in = ProcessService.class.getClassLoader().getResourceAsStream("mailcontent.html");
             InputStreamReader isr = new InputStreamReader(in);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                if (line.equals("<head>")) {
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
                "，可登录系统查看: <a href=\"" + generateRouteUrl(instance.id) + "\">点击跳转至系统(提示：请使用edge浏览器打开，若不幸打开了ie，请复制ie浏览器地址栏，粘贴至edge的地址栏)</a></div><p>以上消息来自" + TAG + "，无需回复</p></html>";
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
                + "，可登录系统查看: <a href=\"" + generateRouteUrl(task.instanceId) + "\">点击跳转至系统(提示：请使用edge浏览器打开，若不幸打开了ie，请复制ie浏览器地址栏，粘贴至edge的地址栏)</a></div><p>" + remark + "</p></html>";
        send(TAG + subject, content,
                Collections.singleton(email), null, null);
    }

    /**
     * 发送审批被取回的邮件通知
     */
    public void sendReclaimMail(ProcessInstance instance, ProcessTask task, Set<String> emailSet) {
        String emails = instance.emails;
        if (StringUtils.hasText(instance.emails)) {
            Set<String> set = Arrays.stream(emails.split(",")).filter(Objects::nonNull)
                    .filter(email -> MAIL_PATTERN.matcher(email).find()).collect(Collectors.toSet());
            emailSet.addAll(set);
        }
        String subject = instance.starterName + "【" + instance.name + "】已被经办【" + task.name + "】取回";
        String content = "<!doctype html><html><div>流程被取回通知：申请单号【" + instance.id + "】。经办：【" + task.name + "】。可登录系统查看: <a href=\"" + generateRouteUrl(task.instanceId) + "\">点击跳转至系统(提示：请使用edge浏览器打开，若不幸打开了ie，请复制ie浏览器地址栏，粘贴至edge的地址栏)</a></div></html>";
        send(TAG + subject, content, emailSet, null, null);
    }

    /**
     * 发送同意审批的邮件
     *
     * @param instance
     * @param task
     * @param processOpinion 审批意见
     */
    @Async
    public void sendApproveMail(ProcessInstance instance, ProcessTask task, ProcessOpinion processOpinion) {
        String emails = instance.emails;
        Set<String> emailSet = Arrays.stream(emails.split(",")).filter(Objects::nonNull)
                .filter(email -> MAIL_PATTERN.matcher(email).find()).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        // 当前审批人审批意见(同意)
        String subject = instance.starterName + "【" + instance.name + "】审批意见";
        String attachmentPart = retrieveAttachments(processOpinion.attachments);
        String content = convertToHtml(instance.id, instance.name, task.name, instance.starterName,
                processOpinion.username, processOpinion.remark, attachmentPart);
        send(TAG + subject, content, emailSet, null, null);
    }

    /**
     * 发送一个邮件通知被退回的环节中负责审批的人或角色：任务被退回
     *
     * @param instance
     * @param task
     * @param processOpinion 审批意见
     * @param emailSet
     */
    public void sendBackMail(ProcessInstance instance, ProcessTask task,
                             ProcessOpinion processOpinion, Set<String> emailSet) {
        if (CollectionUtils.isEmpty(emailSet)) {
            return;
        }
        String subject = instance.starterName + "【" + instance.name + "】被退回";
        String attachmentPart = retrieveAttachments(processOpinion.attachments);
        String content = convertToHtml(instance.id, instance.name, task.name, instance.starterName,
                processOpinion.username, processOpinion.remark, attachmentPart);
        send(TAG + subject, content, emailSet, null, null);
    }

    /**
     * 发送审批被拒绝的邮件，一般来说，被拒绝只需要申请人收到邮件
     *
     * @param instance
     * @param task
     * @param processOpinion
     * @param email
     */
    public void sendRejectMail(ProcessInstance instance, ProcessTask task, ProcessOpinion processOpinion, String email) {
        if (email != null && !MAIL_PATTERN.matcher(email).find()) {
            return;
        }
        String subject = instance.starterName + "【" + instance.name + "】被拒绝";
        String attachmentsPart = retrieveAttachments(processOpinion.attachments);
        String content = convertToHtml(instance.id, instance.name, task.name, instance.starterName,
                processOpinion.username, processOpinion.remark, attachmentsPart);
        send(TAG + subject, content, Collections.singleton(email), null, null);
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
                + "，可登录系统查看: <a href=\"" + generateRouteUrl(instance.id) + "\">点击跳转至系统(提示：请使用edge浏览器打开，若不幸打开了ie，请复制ie浏览器地址栏，粘贴至edge的地址栏)</a></div></html>";
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
     * 获取附件，生成html附件区（不会真的把文件放在邮件附件区，只是放下载链接）
     *
     * @return
     */
    private String retrieveAttachments(String attachments) {
        StringBuilder attachmentsPart = new StringBuilder();
        if (StringUtils.hasText(attachments)) {
            attachmentsPart = new StringBuilder("<ul style='padding:8px 16px;' class='bg-neutral-50'>")
                    .append("<li class=\"li-none\"><span style=\"font-weight: 700\">附件区：</span></li>");
            ArrayNode arrayNode = JsonUtil.readValue(attachments, ArrayNode.class);
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode jsonNode = arrayNode.get(i);
                String fileName = jsonNode.get("name").asText();
                String url = jsonNode.get("url").asText();
                attachmentsPart.append("<li class=\"li-none\"><a style=\"color:#1890ff\" href=\"")
                        .append(url).append("\">").append(fileName).append("</a></li>");
            }
            attachmentsPart.append("</ul>");
        }
        return attachmentsPart.toString();
    }

}
