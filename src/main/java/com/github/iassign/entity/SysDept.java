package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class SysDept {
    @TableId
    public String id;
    public String deptId;
    public String deptCode;// 新的部门代码
    public String deptTw; // 繁体名
    public Date createTime;
    public Date updateTime;
}
