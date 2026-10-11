/*
 * EnglishOverviewServiceTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：EnglishOverviewMapper、EnglishOverviewEntity、EnglishOverviewVO、EnglishOverviewService、EnglishOverviewServiceImpl、EnglishOverviewInvalidException、EnglishOverviewRequestDTO。
 * 阅读入口：clearingOptionalCopyKeepsStageReadOnly（clearingOptionalCopy保留Stage读取Only处理入口）；missingSingletonDoesNotPretendUpdateSucceeded（缺失SingletonDoes不Pretend更新Succeeded处理入口）；rejectsInvalidTitleBeforeWriting（验证拒绝无效标题之前写作）。
 */

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

/**
 * 英语Overview业务服务测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class EnglishOverviewServiceTest {
    /**
     * 数据映射。
     * 协作类型：<code>EnglishOverviewMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock EnglishOverviewMapper mapper;

    /**
     * clearingOptionalCopy保留Stage读取Only处理入口。
     * 主要协作调用：<code>mapper.updateContent</code>、<code>mapper.selectSingleton</code>。
     */
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

    /**
     * 缺失SingletonDoes不Pretend更新Succeeded处理入口。
     * 关键处理步骤调用 <code>service.update</code>、<code>EnglishOverviewRequestDTO.builder</code>。
     */
    @Test
    void missingSingletonDoesNotPretendUpdateSucceeded() {
        EnglishOverviewService service = new EnglishOverviewServiceImpl(mapper);
        assertThatThrownBy(() -> service.update(EnglishOverviewRequestDTO.builder().title("标题").build()))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(404));
    }

    /**
     * 验证拒绝无效标题之前写作。
     * 关键处理步骤调用 <code>service.update</code>、<code>EnglishOverviewRequestDTO.builder</code>、<code>org.mockito.Mockito.verifyNoInteractions</code>。
     */
    @Test
    void rejectsInvalidTitleBeforeWriting() {
        EnglishOverviewService service = new EnglishOverviewServiceImpl(mapper);
        assertThatThrownBy(() -> service.update(EnglishOverviewRequestDTO.builder().title("  ").build()))
                .isInstanceOf(EnglishOverviewInvalidException.class);
        org.mockito.Mockito.verifyNoInteractions(mapper);
    }
}
