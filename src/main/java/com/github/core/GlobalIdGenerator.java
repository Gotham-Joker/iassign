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
