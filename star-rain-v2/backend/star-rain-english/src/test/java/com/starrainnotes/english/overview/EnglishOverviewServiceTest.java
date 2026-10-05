package com.starrainnotes.english.overview;

import com.starrainnotes.english.overview.mapper.EnglishOverviewMapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;

import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

import com.starrainnotes.english.overview.service.EnglishOverviewService;

import com.starrainnotes.english.overview.service.impl.EnglishOverviewServiceImpl;

import com.starrainnotes.english.overview.exception.EnglishOverviewInvalidException;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;

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
        when(mapper.selectSingleton()).thenReturn(row);

        EnglishOverviewVO result = new EnglishOverviewServiceImpl(mapper).update(
                EnglishOverviewRequestDTO.builder()
                        .title(" 新标题 ")
                        .subtitle(" ")
                        .roadmapMarkdown("")
                        .build());

        verify(mapper).updateContent("新标题", null, null, null);
        assertThat(result.getCurrentStage()).isEqualTo("FOUNDATION");
    }

    @Test
    void missingSingletonDoesNotPretendUpdateSucceeded() {
        EnglishOverviewService service = new EnglishOverviewServiceImpl(mapper);
        assertThatThrownBy(() -> service.update(EnglishOverviewRequestDTO.builder().title("标题").build()))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(404));
    }

    @Test
    void rejectsInvalidTitleBeforeWriting() {
        EnglishOverviewService service = new EnglishOverviewServiceImpl(mapper);
        assertThatThrownBy(() -> service.update(EnglishOverviewRequestDTO.builder().title("  ").build()))
                .isInstanceOf(EnglishOverviewInvalidException.class);
        org.mockito.Mockito.verifyNoInteractions(mapper);
    }
}
