package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_message")
public class SysMessage {
  @TableId(type = IdType.ASSIGN_ID)
  public String id;
  public String subject; // 消息主题
  public Integer type; // 消息类型 1-text 2-link 3-card
  public String cover; // card的封面
  public String content; // 消息内容
  public String link; // 链接，暂时只支持站内链接
  public Integer status; // 0-未读 1-已读 2-逻辑删除
  @NotNull(message = "userId is required")
  public String toUserId; // 消息接收人
  public String fromUserId; // 消息发送人id
  public String fromUserAvatar; // 消息发送人头像地址
  public String fromUsername; // 消息发送人姓名
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date createTime; // 消息创建时间
}

