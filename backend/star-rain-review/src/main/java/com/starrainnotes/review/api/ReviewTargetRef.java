/*
 * ReviewTargetRef：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：toKey（转换为键）。
 */

package com.starrainnotes.review.api;

/*
 * 目标业务对象的定位三元组 + 提交时冻结的不可变版本引用。
 *
 * revisionRef 由业务模块生成（例如 revision:7），Review 只负责原样保存与回传：
 * 审核详情必须按这个引用读取当时的视图，不能用目标对象的当前数据代替。
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核目标Ref，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTargetRef {

    /**
     * 目标对象所属模块。
     */
    private String targetModule;
    /**
     * 目标对象的业务类型。
     */
    private String targetType;
    /**
     * 被处理或审核的目标对象标识。
     */
    private Long targetId;
    /**
     * 版本或修订的引用值。
     */
    private String revisionRef;

    /**
     * 转换为键。
     * 关键处理步骤调用 <code>ReviewTargetKey.builder</code>。
     * @return 类型为 <code>ReviewTargetKey</code> 的处理结果
     */
    public ReviewTargetKey toKey() {
        return ReviewTargetKey.builder()
                .targetModule(targetModule)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }
}
