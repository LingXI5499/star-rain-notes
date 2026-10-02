package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * Tag 新增与修改共用的请求体。
 *
 * 修改时 null 表示保持原值，因此不能靠置 null 清空 name / slug（它们本身非空）。
 * description 传空字符串即清空说明。
 */
@Data
public class BlogTagDTO {

    private String slug;
    private String name;
    private String description;
}
