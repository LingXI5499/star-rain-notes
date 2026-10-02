package com.starrainnotes.media.dto;

import com.starrainnotes.media.enumeration.MediaType;
import lombok.Data;

/*
 * MED-002 查询条件。
 *
 * 这里刻意不加 jakarta.validation 注解：
 * 查询接口的合法性由 Service 显式校验并返回 MEDIA_QUERY_INVALID，
 * 避免依赖 @ModelAttribute 的绑定异常类型，也让错误码保持可控、可测。
 */
@Data
public class MediaQueryDTO {

    private int page = 1;

    private int pageSize = 20;

    // 按原始文件名模糊匹配
    private String keyword;

    // IMAGE / DOCUMENT / AUDIO / OTHER
    private String mediaType;

    // ACTIVE / ARCHIVED
    private String status;

    // PUBLIC / PROTECTED
    private String accessLevel;
}
