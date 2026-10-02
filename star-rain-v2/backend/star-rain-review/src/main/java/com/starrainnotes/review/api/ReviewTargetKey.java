package com.starrainnotes.review.api;

/*
 * 目标业务对象的定位三元组。
 *
 * 目标模块用它表达「我要提交/取消/查询哪一个对象」，
 * Review 用它做同一目标只能有一个 PENDING 的判定。
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTargetKey {

    private String targetModule;
    private String targetType;
    private Long targetId;
}
