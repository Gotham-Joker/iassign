package com.github.iassign;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class Constants {
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_PICKER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final Pattern MAIL_PATTERN = Pattern.compile("\\w+@[0-9a-zA-Z.]+");
    public static final String PROCESS_LOGGER = "PROCESS_LOGGER";

    // 流程实例ID
    public static final String INSTANCE_ID = "INSTANCE_ID";

    /*======= 系统内置变量开始 =======*/
    // 流程发起人
    public static final String INSTANCE = "INSTANCE";
    // 流程上下文，用来传递参数，变为全局变量
    public static final String PROCESS_CONTEXT = "CTX";
    // HTTP返回结果
    public static final String HTTP_RESULT = "HTTP_RESULT";
    /*======= 系统内置变量结束 =======*/

}
