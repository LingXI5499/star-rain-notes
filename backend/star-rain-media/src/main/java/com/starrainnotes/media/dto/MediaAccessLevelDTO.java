/*
 * MediaAccessLevelDTO：媒体模块的请求与传输数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.media.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/*
 * 调整媒体访问级别的请求体。
 * 只接受 PUBLIC / PROTECTED，解析失败返回 MEDIA_ACCESS_LEVEL_INVALID。
 */
/**
 * 媒体访问Level传输对象，属于媒体模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class MediaAccessLevelDTO {

    /**
     * 媒体访问级别。
     * 不能为空或仅含空白字符。
     */
    @NotBlank(message = "访问级别不能为空")
    private String accessLevel;
}
