package com.starrainnotes.english.vocabulary.learning;

import com.starrainnotes.english.vocabulary.learning.domain.VocabularyReviewPolicy;
import com.starrainnotes.english.vocabulary.learning.domain.StudyDirectionPolicy;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyReviewPolicyParityTest {

    @Test
    void usesTheTenStageFixedReviewCurve() {
        long[] expected={300L,1_800L,43_200L,86_400L,172_800L,345_600L,604_800L,1_296_000L,2_592_000L,5_184_000L};
        for(int i=0;i<expected.length;i++) assertThat(VocabularyReviewPolicy.intervalSeconds(i+1)).isEqualTo(expected[i]);
        assertThat(VocabularyReviewPolicy.intervalSeconds(0)).isEqualTo(300L);
        assertThat(VocabularyReviewPolicy.intervalSeconds(11)).isEqualTo(5_184_000L);
    }

    @Test
    void preservesTimingBoundariesAndStepCap() {
        VocabularyReviewPolicy policy=new VocabularyReviewPolicy();
        LocalDateTime now=LocalDateTime.of(2026,1,1,12,0);
        assertThat(policy.next(0,null,now).timingStatus()).isEqualTo("NEW");
        assertThat(policy.next(1,now.plusSeconds(1),now).timingStatus()).isEqualTo("EARLY");
        assertThat(policy.next(2,now.minusHours(24),now).timingStatus()).isEqualTo("ON_TIME");
        assertThat(policy.next(3,now.minusHours(24).minusSeconds(1),now).timingStatus()).isEqualTo("OVERDUE");
        assertThat(policy.next(10,null,now).reviewStep()).isEqualTo(10);
    }

    @Test
    void mixedDirectionRemainsDeterministicForWordAndDay() {
        StudyDirectionPolicy policy=new StudyDirectionPolicy();
        assertThat(policy.direction("MIXED",10,100)).isEqualTo("EN_TO_ZH");
        assertThat(policy.direction("MIXED",11,100)).isEqualTo("ZH_TO_EN");
        assertThat(policy.direction("MIXED",10,101)).isEqualTo("ZH_TO_EN");
        assertThat(policy.direction("ZH_TO_EN",10,100)).isEqualTo("ZH_TO_EN");
    }
}
