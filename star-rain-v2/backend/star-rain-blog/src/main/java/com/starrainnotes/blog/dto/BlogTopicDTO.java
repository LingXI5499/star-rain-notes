package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * Topic 新增与修改共用的请求体。
 * 与 BlogTagDTO 的唯一差异在语义：Topic 的 name 不做唯一校验。
 */
@Data
public class BlogTopicDTO {

    private String slug;
    private String name;
    private String description;
}
