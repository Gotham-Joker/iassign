package com.github.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {
    public static final String CN_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static String format(Date date,String pattern) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 中国大陆日期格式
     * @param date
     * @return
     */
    public static String formatCn(Date date) {
        return format(date, CN_DATE_FORMAT);
    }
}
