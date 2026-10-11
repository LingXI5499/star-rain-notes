/*
 * VocabularyServiceTest：英语学习模块的测试文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：Page、Word、VocabularyMapper、VocabularyServiceImpl。
 * 阅读入口：givenThemeHas（given主题是否具备处理入口）；givenAudiosAreEmpty（givenAudiosAre空白处理入口）；word（单词处理入口）；pageAndSizeBecomeOffsetAndLimit（分页读取及大小Become偏移及上限）。
 */

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
import com.starrainnotes.english.vocabulary.service.impl.VocabularyServiceImpl;
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
/**
 * 词汇业务服务测试，属于英语学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    /**
     * 数据映射。
     * 协作类型：<code>VocabularyMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock VocabularyMapper mapper;

    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks VocabularyServiceImpl service;

    /* attachAudios 对空列表直接返回，因此只有真的取到词时才需要 stub audios */
    /**
     * given主题是否具备处理入口。
     * 主要协作调用：<code>mapper.countWords</code>。
     *
     * @param total 符合查询条件的总记录数；类型为 <code>long</code>
     */
    private void givenThemeHas(long total) {
        when(mapper.countWords(eq(1L), eq(""))).thenReturn(total);
    }

    /**
     * givenAudiosAre空白处理入口。
     * 主要协作调用：<code>mapper.audios</code>。
     */
    private void givenAudiosAreEmpty() {
        when(mapper.audios(anyList())).thenReturn(List.of());
    }

    /* 必须带 id：attachAudios 会先收集非空 id，没有 id 的词根本不会去查音频 */
    /**
     * 单词处理入口。
     * 关键处理步骤调用 <code>word.setId</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    private static Word word(long id) {
        Word word = new Word();
        word.setId(id);
        return word;
    }

    /**
     * 分页读取及大小Become偏移及上限。
     * 主要协作调用：<code>mapper.words</code>、<code>service.words</code>。
     */
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

    /**
     * 分页读取Below单次FallsBack目标首次分页。
     * 主要协作调用：<code>mapper.words</code>、<code>service.words</code>。
     */
    @Test
    void pageBelowOneFallsBackToFirstPage() {
        givenThemeHas(170L);
        when(mapper.words(eq(1L), eq(""), eq(0L), eq(20))).thenReturn(List.of());

        Page<Word> page = service.words("1", null, 0, 20);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getItems()).isEmpty();
    }

    /**
     * 大小是否Clamped目标单次HundredSo单次请求CannotDumpTheLibrary处理入口。
     * 主要协作调用：<code>mapper.words</code>、<code>service.words</code>。
     */
    @Test
    void sizeIsClampedToOneHundredSoOneRequestCannotDumpTheLibrary() {
        givenThemeHas(7053L);
        when(mapper.words(eq(1L), eq(""), eq(0L), eq(100))).thenReturn(List.of());

        Page<Word> page = service.words("1", null, 1, 5000);

        verify(mapper).words(eq(1L), eq(""), eq(0L), eq(100));
        assertThat(page.getSize()).isEqualTo(100);
    }

    /**
     * 大小ZeroStillReturns单次行Instead所属Failing处理入口。
     * 主要协作调用：<code>mapper.words</code>、<code>service.words</code>。
     */
    @Test
    void sizeZeroStillReturnsOneRowInsteadOfFailing() {
        givenThemeHas(170L);
        when(mapper.words(eq(1L), eq(""), eq(0L), eq(1))).thenReturn(List.of());

        Page<Word> page = service.words("1", null, 1, 0);

        assertThat(page.getSize()).isEqualTo(1);
    }

    /**
     * 空白结果SkipsThe查询参数及ReportsZeroPages处理入口。
     * 主要协作调用：<code>mapper.countWords</code>、<code>service.words</code>。
     */
    @Test
    void emptyResultSkipsTheQueryAndReportsZeroPages() {
        when(mapper.countWords(eq(1L), eq(""))).thenReturn(0L);

        Page<Word> page = service.words("1", null, 1, 24);

        assertThat(page.getItems()).isEmpty();
        assertThat(page.getTotalPages()).isZero();
        verify(mapper, never()).words(anyLong(), any(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    /**
     * 搜索Term是否Trimmed之前ReachingSql处理入口。
     * 主要协作调用：<code>mapper.countWords</code>、<code>mapper.words</code>、<code>service.words</code>。
     */
    @Test
    void searchTermIsTrimmedBeforeReachingSql() {
        when(mapper.countWords(eq(null), eq("physical"))).thenReturn(1L);
        when(mapper.words(eq(null), eq("physical"), eq(0L), eq(24))).thenReturn(List.of(word(1L)));
        givenAudiosAreEmpty();

        service.words(null, "  physical  ", 1, 24);

        verify(mapper).countWords(eq(null), eq("physical"));
    }

    /**
     * 批量查找HonoursItsOwn上限处理入口。
     * 主要协作调用：<code>service.wordsByIds</code>。
     */
    @Test
    void batchLookupHonoursItsOwnLimit() {
        service.wordsByIds(List.of(1L, 2L, 3L), 0);

        verify(mapper, never()).wordsByIds(anyList());
    }
}
