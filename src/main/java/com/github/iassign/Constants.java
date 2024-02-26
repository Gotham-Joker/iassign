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
