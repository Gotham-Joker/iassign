package com.github.iassign.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ProcessInstanceStatus implements IEnum<Integer> {
    CANCEL(0), // 废弃、撤回、取消
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
    REJECTED(4), // 拒绝
    RETURN(5); // 退回至申请人

    private final Integer status;

    ProcessInstanceStatus(Integer status) {
        this.status = status;
    }

    @Override
    public Integer getValue() {
        return status;
    }
}
