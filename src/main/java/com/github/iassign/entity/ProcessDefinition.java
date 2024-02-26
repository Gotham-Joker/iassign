/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 流程模型，保存着DAG图的json内容
 */
@Data
@TableName("t_process_definition")
public class ProcessDefinition {
    @TableId(type = IdType.ASSIGN_ID)
    public String id;
    public String name;
    public String seqNo; // 序号，有时候记住序号比记住流程名有用，例如0001=>休假申请
    public String icon;
    public String description;
    public Boolean status; // 部署状态 0-未部署 1-已部署
    public Boolean returnable; // 是否允许回退至发起人 0-不允许 1-允许
    // 不是数据库字段
    @TableField(exist = false)
    public String dag;
    public String ruId; // 一直保存着最新的运行时dag Id
    public String formId;  // 绑定的表单ID  可以建立搜索
    public String creator;
    public String groupName;
    public String managers; // 管理者id列表
    public String fallback; // 流程失败时执行的接口回调
    public Date createTime;
    public Date updateTime;
}
