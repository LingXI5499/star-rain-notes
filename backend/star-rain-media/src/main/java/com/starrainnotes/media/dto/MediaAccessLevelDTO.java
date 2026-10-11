package com.starrainnotes.media.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/*
 * 调整媒体访问级别的请求体。
 * 只接受 PUBLIC / PROTECTED，解析失败返回 MEDIA_ACCESS_LEVEL_INVALID。
 */
@Data
public class MediaAccessLevelDTO {

    @NotBlank(message = "访问级别不能为空")
    private String accessLevel;
}
