/*
 * ReviewStatusTest：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：onlyPendingIsNotTerminal（PENDING \u4e0d\u662f\u7ec8\u6001\uff0c\u5176\u4f59\u4e09\u4e2a\u72b6\u6001\u90fd\u662f\u7ec8\u6001）；isKnownAcceptsOnlyFourStates（isKnown \u53ea\u8ba4\u56db\u4e2a\u5408\u6cd5\u72b6\u6001\uff0c\u5927\u5c0f\u5199\u654f\u611f\u4e14\u62d2\u7edd\u672a\u77e5\u503c）；codesMatchEnumNames（\u72b6\u6001\u7801\u5e38\u91cf\u4e0e\u679a\u4e3e\u540d\u4e00\u81f4\uff1a\u6570\u636e\u5e93\u3001XML\u3001Java \u4e09\u5904\u4e0d\u4f1a\u6f02\u79fb）；terminalStatesAreDistinct（\u7ec8\u6001\u96c6\u5408\u4e92\u65a5\uff1a\u4efb\u610f\u4e24\u4e2a\u7ec8\u6001\u90fd\u4e0d\u76f8\u7b49\uff0c\u907f\u514d\u5206\u652f\u5199\u6210 || \u65f6\u6f0f\u5224）。
 */

package com.starrainnotes.review.enumeration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * 审核状态机测试。
 *
 * 状态机是这个模块唯一的「真相来源」，所以把它单独测透：
 *   PENDING --approve/reject/cancel--> APPROVED/REJECTED/CANCELED
 * 只有 PENDING 可变更，其余三个都是终态且不可回退。
 *
 * 这里用枚举的 isTerminal / isKnown 做判定，与数据库里的 status 列取同一套字符串常量，
 * 避免出现「Java 里判 PENDING、SQL 里写成 pending」这种大小写漂移。
 */
/**
 * 审核状态测试，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class ReviewStatusTest {

    /**
     * PENDING \u4e0d\u662f\u7ec8\u6001\uff0c\u5176\u4f59\u4e09\u4e2a\u72b6\u6001\u90fd\u662f\u7ec8\u6001。
     * 关键处理步骤调用 <code>ReviewStatus.isTerminal</code>。
     */
    @Test
    @DisplayName("PENDING 不是终态，其余三个状态都是终态")
    void onlyPendingIsNotTerminal() {
        assertThat(ReviewStatus.isTerminal(ReviewStatus.PENDING_CODE)).isFalse();
        assertThat(ReviewStatus.isTerminal(ReviewStatus.APPROVED_CODE)).isTrue();
        assertThat(ReviewStatus.isTerminal(ReviewStatus.REJECTED_CODE)).isTrue();
        assertThat(ReviewStatus.isTerminal(ReviewStatus.CANCELED_CODE)).isTrue();
    }

    /**
     * isKnown \u53ea\u8ba4\u56db\u4e2a\u5408\u6cd5\u72b6\u6001\uff0c\u5927\u5c0f\u5199\u654f\u611f\u4e14\u62d2\u7edd\u672a\u77e5\u503c。
     * 关键处理步骤调用 <code>ReviewStatus.isKnown</code>。
     */
    @Test
    @DisplayName("isKnown 只认四个合法状态，大小写敏感且拒绝未知值")
    void isKnownAcceptsOnlyFourStates() {
        assertThat(ReviewStatus.isKnown(ReviewStatus.PENDING_CODE)).isTrue();
        assertThat(ReviewStatus.isKnown(ReviewStatus.APPROVED_CODE)).isTrue();
        assertThat(ReviewStatus.isKnown(ReviewStatus.REJECTED_CODE)).isTrue();
        assertThat(ReviewStatus.isKnown(ReviewStatus.CANCELED_CODE)).isTrue();

        assertThat(ReviewStatus.isKnown("pending")).isFalse();
        assertThat(ReviewStatus.isKnown("CANCELLED")).isFalse();
        assertThat(ReviewStatus.isKnown("DELETED")).isFalse();
        assertThat(ReviewStatus.isKnown(null)).isFalse();
    }

    /**
     * \u72b6\u6001\u7801\u5e38\u91cf\u4e0e\u679a\u4e3e\u540d\u4e00\u81f4\uff1a\u6570\u636e\u5e93\u3001XML\u3001Java \u4e09\u5904\u4e0d\u4f1a\u6f02\u79fb。
     * 关键处理步骤调用 <code>List.of</code>、<code>ReviewStatus.values</code>、<code>ReviewStatus.PENDING.name</code>、<code>ReviewStatus.APPROVED.name</code>。
     */
    @Test
    @DisplayName("状态码常量与枚举名一致：数据库、XML、Java 三处不会漂移")
    void codesMatchEnumNames() {
        List<ReviewStatus> statuses = List.of(ReviewStatus.values());
        assertThat(statuses).extracting(Enum::name)
                .containsExactly("PENDING", "APPROVED", "REJECTED", "CANCELED");
        assertThat(ReviewStatus.PENDING_CODE).isEqualTo(ReviewStatus.PENDING.name());
        assertThat(ReviewStatus.APPROVED_CODE).isEqualTo(ReviewStatus.APPROVED.name());
        assertThat(ReviewStatus.REJECTED_CODE).isEqualTo(ReviewStatus.REJECTED.name());
        assertThat(ReviewStatus.CANCELED_CODE).isEqualTo(ReviewStatus.CANCELED.name());
    }

    /**
     * \u7ec8\u6001\u96c6\u5408\u4e92\u65a5\uff1a\u4efb\u610f\u4e24\u4e2a\u7ec8\u6001\u90fd\u4e0d\u76f8\u7b49\uff0c\u907f\u514d\u5206\u652f\u5199\u6210 || \u65f6\u6f0f\u5224。
     */
    @Test
    @DisplayName("终态集合互斥：任意两个终态都不相等，避免分支写成 || 时漏判")
    void terminalStatesAreDistinct() {
        assertThat(ReviewStatus.APPROVED_CODE).isNotEqualTo(ReviewStatus.REJECTED_CODE);
        assertThat(ReviewStatus.REJECTED_CODE).isNotEqualTo(ReviewStatus.CANCELED_CODE);
        assertThat(ReviewStatus.APPROVED_CODE).isNotEqualTo(ReviewStatus.CANCELED_CODE);
    }
}
