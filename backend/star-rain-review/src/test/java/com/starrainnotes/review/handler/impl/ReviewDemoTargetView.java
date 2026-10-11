/*
 * ReviewDemoTargetView：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ReviewTargetView。
 * 阅读入口：viewType（view类型处理入口）。
 */

package com.starrainnotes.review.handler.impl;

import com.starrainnotes.review.api.ReviewTargetView;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 演示用目标视图。
 *
 * 仅供 Review 单元测试断言冻结视图；生产环境使用 Tutorial 的视图。
 *
 * 字段刻意保持业务无关：它证明 Review 只搬运视图、不解析业务字段。
 */
/**
 * 审核Demo目标View，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDemoTargetView implements ReviewTargetView {

    /**
     * view类型。
     */
    private String viewType;

    /**
     * 被处理或审核的目标对象标识。
     */
    private Long targetId;
    /**
     * 目标对象所属模块。
     */
    private String targetModule;
    /**
     * 目标对象的业务类型。
     */
    private String targetType;
    /**
     * 版本或修订的引用值。
     */
    private String revisionRef;

    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;

    // 演示用版本内容，真实业务模块这里放的是章节列表、正文等
    /**
     * 内容快照。
     */
    private String contentSnapshot;

    /**
     * frozen时间。
     */
    private LocalDateTime frozenAt;

    /**
     * view类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String viewType() {
        return viewType;
    }
}
