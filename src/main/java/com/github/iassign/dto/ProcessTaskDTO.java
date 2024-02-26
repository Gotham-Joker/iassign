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

package com.github.iassign.dto;

import com.github.iassign.enums.TaskOperation;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class ProcessTaskDTO {
    @NotEmpty(message = "taskId must be supplied")
    public String taskId; // 任务id
    public Map<String, Object> formData; // 额外填写的表单
    public Map<String, Object> variables; // 任务变量
    private String remark;  // 备注
    public String attachments; // 附件地址
    public String emails; // 邮件接收人列表
    public TaskOperation operation; // 操作: 同意，回退，拒绝，退回发起人
    public String backwardTaskId; // 回退到哪个任务


    public String safeRemark() {
        if (StringUtils.hasText(remark)) {
            remark = remark.replaceAll("style=\"text-align:\\s*center;?\"", "class=\"text-center\"")
                    .replaceAll("style=\"text-align:\\s*left;?\"", "class=\"text-start\"")
                    .replaceAll("style=\"text-align:\\s*right;?\"", "class=\"text-end\"")
                    .replaceAll("alt=\"\" data-href=\"emo-i\" style=\"\"", "class=\"emo-i\"");
//            "<img src=\"http://localhost:8080/2.png\" alt=\"\" data-href=\"\" style=\"width: 50%;\">"
            Pattern pattern = Pattern.compile("<img src=.*?style=\"width: (?<width>\\d+)%;\">");
            Matcher matcher = pattern.matcher(remark);
            while (matcher.find()) {
                String img = matcher.group(0);
                String width = matcher.group("width");
                String imgNew = img.replaceAll("style=\"width: \\d+%;\"", "class=\"w-" + width + "p\"");
                remark = remark.replace(img, imgNew);
            }
        }
        return remark;
    }
}
