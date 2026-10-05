package com.starrainnotes.english.vocabulary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.mapper.VocabularyMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * 分页契约。
 *
 * 这里锁的是「偏移量真的传给了 SQL」：之前公开接口只认 size，调用方传 pageSize 时会静默回落到
 * 默认 50 条，看起来像分页没生效。参数别名的解析在 Controller，服务层负责钳位与偏移量计算，
 * 因此这一层必须证明 offset/limit 的值与 page/size 一致。
 */
@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    @Mock VocabularyMapper mapper;

    @InjectMocks VocabularyService service;

    /* attachAudios 对空列表直接返回，因此只有真的取到词时才需要 stub audios */
    private void givenThemeHas(long total) {
        when(mapper.countWords(eq(1L), eq(""))).thenReturn(total);
    }

    private void givenAudiosAreEmpty() {
        when(mapper.audios(anyList())).thenReturn(List.of());
    }

    /* 必须带 id：attachAudios 会先收集非空 id，没有 id 的词根本不会去查音频 */
    private static Word word(long id) {
        Word word = new Word();
        word.setId(id);
        return word;
    }

    @Test
    void pageAndSizeBecomeOffsetAndLimit() {
        givenThemeHas(170L);
        givenAudiosAreEmpty();
        when(mapper.words(eq(1L), eq(""), eq(4L), eq(2))).thenReturn(List.of(word(1L)));

        Page<Word> page = service.words("1", null, 3, 2);

        verify(mapper).words(eq(1L), eq(""), eq(4L), eq(2));
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getPage()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotal()).isEqualTo(170);
        assertThat(page.getTotalPages()).isEqualTo(85);
    }

    @Test
    void pageBelowOneFallsBackToFirstPage() {
        givenThemeHas(170L);
        when(mapper.words(eq(1L), eq(""), eq(0L), eq(20))).thenReturn(List.of());

        Page<Word> page = service.words("1", null, 0, 20);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    void sizeIsClampedToOneHundredSoOneRequestCannotDumpTheLibrary() {
        givenThemeHas(7053L);
        when(mapper.words(eq(1L), eq(""), eq(0L), eq(100))).thenReturn(List.of());

        Page<Word> page = service.words("1", null, 1, 5000);

        verify(mapper).words(eq(1L), eq(""), eq(0L), eq(100));
        assertThat(page.getSize()).isEqualTo(100);
    }

    @Test
    void sizeZeroStillReturnsOneRowInsteadOfFailing() {
        givenThemeHas(170L);
        when(mapper.words(eq(1L), eq(""), eq(0L), eq(1))).thenReturn(List.of());

        Page<Word> page = service.words("1", null, 1, 0);

        assertThat(page.getSize()).isEqualTo(1);
    }

    @Test
    void emptyResultSkipsTheQueryAndReportsZeroPages() {
        when(mapper.countWords(eq(1L), eq(""))).thenReturn(0L);

        Page<Word> page = service.words("1", null, 1, 24);

        assertThat(page.getItems()).isEmpty();
        assertThat(page.getTotalPages()).isZero();
        verify(mapper, never()).words(anyLong(), any(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void searchTermIsTrimmedBeforeReachingSql() {
        when(mapper.countWords(eq(null), eq("physical"))).thenReturn(1L);
        when(mapper.words(eq(null), eq("physical"), eq(0L), eq(24))).thenReturn(List.of(word(1L)));
        givenAudiosAreEmpty();

        service.words(null, "  physical  ", 1, 24);

        verify(mapper).countWords(eq(null), eq("physical"));
    }

    @Test
    void batchLookupHonoursItsOwnLimit() {
        service.wordsByIds(List.of(1L, 2L, 3L), 0);

        verify(mapper, never()).wordsByIds(anyList());
    }
}
