/*
 * BlogTopicOrderItem：博客模块的请求与传输数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 专题成员排序的批量更新项。
 *
 * 直接持有最终 sortOrder，而不是借助 CASE WHEN 拼 SQL：
 * MyBatis 的 foreach 批量 UPDATE 更容易读，也便于在测试里断言写入的序号。
 */
/**
 * 博客专题顺序Item，属于博客模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicOrderItem {

    /**
     * 文章标识。
     */
    private Long postId;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
}
