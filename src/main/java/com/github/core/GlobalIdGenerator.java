package com.github.core;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Sequence;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * 全局分布式ID生成器
 * mybatis plus自带的{@link com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator}生成器,
 * 单机运行不容易出问题，但是如果是k8s集群，就很容易生成一样的id，必须为每台节点设置datacenterId和workerId，
 * 这两个值都必须小于32
 */
@Slf4j
public class GlobalIdGenerator implements IdentifierGenerator {
    private Sequence sequence;

    public GlobalIdGenerator() {
        int workerId = new Random().nextInt(32);
        int dataCenterId = new Random().nextInt(32);
        log.info("globalIdGenerator, workerId:{},dataCenterId:{}", workerId, dataCenterId);
        sequence = new Sequence(workerId, dataCenterId);
    }

    @Override
    public Number nextId(Object entity) {
        return sequence.nextId();
    }

    public String nextIdStr() {
        return sequence.nextId() + "";
    }
}
