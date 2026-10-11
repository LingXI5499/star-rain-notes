/*
 * BlogPostNeighborVO：博客模块的响应数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 相邻文章只传导航所需字段，不把正文或后台状态带到公开详情。
/**
 * 博客文章Neighbor响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostNeighborVO {

    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
}
