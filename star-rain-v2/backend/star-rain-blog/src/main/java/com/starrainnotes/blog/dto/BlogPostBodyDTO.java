package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * BLOG-003 单独更新正文。
 *
 * 正文里的媒体引用由 Service 从 Markdown 里的公开内容地址解析，
 * 不额外收一份 id 列表：两份真源一定会漂移，谁对谁错无法判断。
 */
@Data
public class BlogPostBodyDTO {

    private String bodyMarkdown;
}
