package com.github.iassign.vo;

import lombok.Data;

@Data
public class CheckedListQuery {
    // 已办理用户ID
    public String userId;
    // 申请单号
    public String instanceId;
    public String createTimeGe;
    public String createTimeLe;
}
