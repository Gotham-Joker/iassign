package com.github.iassign.core.util;

import com.github.core.JsonUtil;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceHolderUtils {

    /**
     * 进行值替换，值必须是Map类型或者提供了get方法的类
     *
     * @param str
     * @param variables
     * @return
     */
    public static String replace(String str, Map<String, Object> variables) {
        // 处理格式化成json的需求
        Pattern jsonPattern = Pattern.compile("\\$\\{JSON\\((?<express>[\\w.]+?)\\)\\}");
        Matcher matcher = jsonPattern.matcher(str);
        while (matcher.find()) {
            Object result = evaluate(variables, matcher);
            str = str.replace(matcher.group(0), JsonUtil.toJson(result));
        }

        // 处理普通的取值需求
        Pattern pattern = Pattern.compile("\\$\\{(?<express>[\\w.]+?)\\}");
        matcher = pattern.matcher(str);
        while (matcher.find()) {
            Object result = evaluate(variables, matcher);
            if (result == null) {
                continue;
            }
            str = str.replace(matcher.group(0), String.valueOf(result));
        }
        return str;
    }

    private static Object evaluate(Map<String, Object> variables, Matcher matcher) {
        String express = matcher.group("express");
        String[] splits = express.split("\\.");
        Object obj = variables.get(splits[0]);
        for (int i = 1; i < splits.length; i++) {
            if (obj instanceof Map) {
                obj = ((Map<?, ?>) obj).get(splits[i]);
            } else {
                String property = splits[i];
                Method method = ReflectionUtils.findMethod(obj.getClass(), "get" + firstCharToUpperCase(property));
                try {
                    obj = method.invoke(obj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return obj;
    }

    public static String firstCharToUpperCase(String property) {
        if (!StringUtils.hasText(property)) {
            return property;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(property.charAt(0)));
        int length = property.length();
        for (int i = 1; i < length; i++) {
            char c = property.charAt(i);
            sb.append(c);
        }
        return sb.toString();
    }


}
