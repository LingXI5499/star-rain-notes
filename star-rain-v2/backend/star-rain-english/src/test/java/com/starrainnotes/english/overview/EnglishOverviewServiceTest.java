package com.starrainnotes.english.overview;

import com.starrainnotes.english.overview.mapper.EnglishOverviewMapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;

import com.starrainnotes.english.overview.dto.EnglishOverviewView;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import com.starrainnotes.english.overview.exception.EnglishOverviewInvalidException;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnglishOverviewServiceTest {
    @Mock EnglishOverviewMapper mapper;

    @Test
    void clearingOptionalCopyKeepsStageReadOnly() {
        EnglishOverviewEntity row = new EnglishOverviewEntity();
        row.setId(1);
        row.setTitle("英语能力成长路径");
        row.setCurrentStage("FOUNDATION");
        when(mapper.updateContent("新标题", null, null, null)).thenReturn(1);
        when(mapper.selectById(1)).thenReturn(row);

        EnglishOverviewView result = new EnglishOverviewService(mapper).update(
                new EnglishOverviewRequest(" 新标题 ", " ", null, ""));

        verify(mapper).updateContent("新标题", null, null, null);
        assertThat(result.currentStage()).isEqualTo("FOUNDATION");
    }

    @Test
    void missingSingletonDoesNotPretendUpdateSucceeded() {
        EnglishOverviewService service = new EnglishOverviewService(mapper);
        assertThatThrownBy(() -> service.update(new EnglishOverviewRequest("标题", null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(404));
    }

    @Test
    void rejectsInvalidTitleBeforeWriting() {
        EnglishOverviewService service = new EnglishOverviewService(mapper);
        assertThatThrownBy(() -> service.update(new EnglishOverviewRequest("  ", null, null, null)))
                .isInstanceOf(EnglishOverviewInvalidException.class);
        org.mockito.Mockito.verifyNoInteractions(mapper);
    }
}
