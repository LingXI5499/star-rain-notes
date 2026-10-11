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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTargetRef {

    private String targetModule;
    private String targetType;
    private Long targetId;
    private String revisionRef;

    public ReviewTargetKey toKey() {
        return ReviewTargetKey.builder()
                .targetModule(targetModule)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }
}
