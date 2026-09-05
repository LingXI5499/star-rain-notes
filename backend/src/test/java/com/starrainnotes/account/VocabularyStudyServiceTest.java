package com.starrainnotes.account;

import com.starrainnotes.account.english.vocabulary.VocabularyStudyService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VocabularyStudyServiceTest {

    @Test
    void usesTheTenStageFixedReviewCurve() {
        assertThat(VocabularyStudyService.intervalSeconds(1)).isEqualTo(300L);
        assertThat(VocabularyStudyService.intervalSeconds(2)).isEqualTo(1_800L);
        assertThat(VocabularyStudyService.intervalSeconds(3)).isEqualTo(43_200L);
        assertThat(VocabularyStudyService.intervalSeconds(4)).isEqualTo(86_400L);
        assertThat(VocabularyStudyService.intervalSeconds(5)).isEqualTo(172_800L);
        assertThat(VocabularyStudyService.intervalSeconds(6)).isEqualTo(345_600L);
        assertThat(VocabularyStudyService.intervalSeconds(7)).isEqualTo(604_800L);
        assertThat(VocabularyStudyService.intervalSeconds(8)).isEqualTo(1_296_000L);
        assertThat(VocabularyStudyService.intervalSeconds(9)).isEqualTo(2_592_000L);
        assertThat(VocabularyStudyService.intervalSeconds(10)).isEqualTo(5_184_000L);
        assertThat(VocabularyStudyService.intervalSeconds(11)).isEqualTo(5_184_000L);
    }
}
