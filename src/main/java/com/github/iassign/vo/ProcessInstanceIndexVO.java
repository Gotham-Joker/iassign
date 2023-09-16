package com.github.iassign.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.iassign.entity.ProcessInstanceIndex;
import lombok.Data;

import java.util.Date;

@Data
public class ProcessInstanceIndexVO extends ProcessInstanceIndex {
    // 指定时间范围
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date createTimeGe;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date createTimeLe;

    /**
     * 流程实例状态
     */
    public String status;
}
