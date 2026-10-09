package com.starrainnotes.tutorial.content.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.tutorial.content.utils.TutorialContentMediaParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TutorialMediaReferencesTest {
    @Test
    void draftAndFrozenRevisionKeepIndependentMediaReferences() {
        MediaReferenceApi media = mock(MediaReferenceApi.class);
        TutorialMediaReferences references = new TutorialMediaReferences(media, mock(com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper.class), mock(com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper.class));
        String body = "![图](/api/media/assets/12/content) ![重复](/api/media/assets/12/content)";

        references.replaceChapter(20L, body);
        references.attachRevision(30L, List.of(body));
        references.replaceChapter(20L, "# 图片已从草稿移除");

        verify(media, times(2)).detachAll("TUTORIAL", "CHAPTER", 20L);
        ArgumentCaptor<MediaReferenceCommand> commands = ArgumentCaptor.forClass(MediaReferenceCommand.class);
        verify(media, times(2)).attach(commands.capture());
        List<MediaReferenceCommand> attached = new ArrayList<>(commands.getAllValues());
        assertThat(attached).extracting(MediaReferenceCommand::getSourceType).containsExactly("CHAPTER", "REVISION");
        assertThat(attached).extracting(MediaReferenceCommand::getSourceId).containsExactly(20L, 30L);
        assertThat(attached).extracting(MediaReferenceCommand::getMediaAssetId).containsExactly(12L, 12L);
    }

    @Test
    void onlyLocalContentUrlsBecomeReferences() {
        assertThat(TutorialContentMediaParser.extractMediaAssetIds("""
                ![图](/api/media/assets/7/content)
                ![外部](https://cdn.example.com/api/media/assets/8/content.png)
                ![图2](/api/media/assets/7/content)
                """)).containsExactly(7L);
    }
    @Test
    void workspaceProtectsMediaUsedByCardsAndQuestionsIncludingDisabledDraftContent() {
        var media=mock(MediaReferenceApi.class);
        var cards=mock(com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper.class);
        var questions=mock(com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper.class);
        var card=new com.starrainnotes.tutorial.content.entity.TutorialKnowledgeCardEntity();
        card.setStatus("DISABLED");card.setBackMarkdown("![draft](/api/media/assets/21/content)");
        var question=new com.starrainnotes.tutorial.content.entity.TutorialQuestionEntity();
        question.setReferenceAnswer("![answer](/api/media/assets/22/content)");
        org.mockito.Mockito.when(cards.listByChapterId(4L)).thenReturn(List.of(card));
        org.mockito.Mockito.when(questions.listByChapterId(4L)).thenReturn(List.of(question));
        new TutorialMediaReferences(media,cards,questions).replaceChapter(4L,"![body](/api/media/assets/20/content)");
        var captured=ArgumentCaptor.forClass(MediaReferenceCommand.class);verify(media,times(3)).attach(captured.capture());
        assertThat(captured.getAllValues()).extracting(MediaReferenceCommand::getMediaAssetId).containsExactly(20L,21L,22L);
    }

}
