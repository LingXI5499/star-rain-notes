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
class ReviewStatusTest {

    @Test
    @DisplayName("PENDING 不是终态，其余三个状态都是终态")
    void onlyPendingIsNotTerminal() {
        assertThat(ReviewStatus.isTerminal(ReviewStatus.PENDING_CODE)).isFalse();
        assertThat(ReviewStatus.isTerminal(ReviewStatus.APPROVED_CODE)).isTrue();
        assertThat(ReviewStatus.isTerminal(ReviewStatus.REJECTED_CODE)).isTrue();
        assertThat(ReviewStatus.isTerminal(ReviewStatus.CANCELED_CODE)).isTrue();
    }

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

    @Test
    @DisplayName("终态集合互斥：任意两个终态都不相等，避免分支写成 || 时漏判")
    void terminalStatesAreDistinct() {
        assertThat(ReviewStatus.APPROVED_CODE).isNotEqualTo(ReviewStatus.REJECTED_CODE);
        assertThat(ReviewStatus.REJECTED_CODE).isNotEqualTo(ReviewStatus.CANCELED_CODE);
        assertThat(ReviewStatus.APPROVED_CODE).isNotEqualTo(ReviewStatus.CANCELED_CODE);
    }
}
