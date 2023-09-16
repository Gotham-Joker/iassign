package com.github.iassign.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ProcessTaskStatus implements IEnum<Integer> {
    /**
     * 运行中，系统节点会采用这个状态
     */
    RUNNING(0),
    /**
     * 待受理
     */
    PENDING(1),
    /**
     * 已受理，已认领
     */
    CLAIMED(2),
    /**
     * 已指派
     */
    ASSIGNED(3),
    /**
     * 已同意
     */
    SUCCESS(4),
    /**
     * 被拒绝
     */
    REJECTED(5),
    /**
     * 回退
     */
    BACK(6),
    /**
     * 失败
     */
    FAILED(7);

    private final Integer status;

    ProcessTaskStatus(Integer status) {
        this.status = status;
    }

    @Override
    public Integer getValue() {
        return status;
    }
}
