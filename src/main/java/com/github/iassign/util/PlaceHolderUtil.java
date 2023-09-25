package com.github.iassign.util;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceHolderUtil {

    /**
     * 值替换，访问属性时，该引用必须是map类型或提供get方法
     *
     * @param str
     * @param variables
     * @return
     */
    public static String replace(String str, Map<String, Object> variables) {
        Pattern pattern = Pattern.compile("\\$\\{(?<express>[\\w.]+?)\\}");
        String tmp = str;
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String express = matcher.group("express");
            String[] splits = express.split("\\.");
            Object obj = variables.get(splits[0]);
            for (int i = 1; i < splits.length - 1; i++) {
                if (obj instanceof Map<?, ?>) {
                    obj = ((Map<?, ?>) obj).get(splits[i]);
                } else {
                    String property = splits[i];
                    Method method = ReflectionUtils.findMethod(obj.getClass(), "get" + firstCharToUppercase(property));
                    try {
                        method.invoke(obj, splits[i + 1]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (obj == null) {
                obj = "";
            }
            tmp = tmp.replace(matcher.group(0), String.valueOf(obj));
        }
        return tmp;
    }

    private static String firstCharToUppercase(String property) {
        if (!StringUtils.hasText(property)) {
            return property;
        }
        StringBuilder sb = new StringBuilder(Character.toUpperCase(property.charAt(0)));
        int length = property.length();
        for (int i = 1; i < length; i++) {
            char c = property.charAt(i);
            sb.append(c);
        }
        return sb.toString();
    }
}
