package com.github.iassign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ProcessLogger {

    /**
     * 根据流程实例获取日志，用于日志路由
     *
     * @param instanceId
     * @return
     */
    public static Logger logger(String instanceId) {
        MDC.put(Constants.INSTANCE_ID, instanceId);
        return LoggerFactory.getLogger(Constants.PROCESS_LOGGER);
    }
}
