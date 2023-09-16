package com.github.iassign;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class Constants {
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final Pattern MAIL_PATTERN = Pattern.compile("\\w+@[0-9a-zA-Z.]+");
    public static final String PROCESS_LOGGER = "PROCESS_LOGGER";

    // 流程实例ID
    public static final String INSTANCE_ID = "INSTANCE_ID";
    // 流程发起人
    public static final String INSTANCE = "INSTANCE";
}
