package com.github.iassign.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 操作类型
 */
public enum TaskOperation implements IEnum<Integer> {
    REJECT(0), // 否决
    APPROVE(1), // 同意
    BACK(2), // 退回
    ASSIGN(3), // 指派
    RETURN(4) // 退回到发起人
    ;
    private final Integer value;

    TaskOperation(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }
}
