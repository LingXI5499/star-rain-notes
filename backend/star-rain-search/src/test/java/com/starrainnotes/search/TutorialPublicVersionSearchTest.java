/*
 * TutorialPublicVersionSearchTest：搜索模块的测试文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：PageResult、SearchIndexApi、SearchDocumentMapper、SearchRebuildServiceImpl、SearchTextExtractor。
 * 阅读入口：rebuildRemovesWithdrawnChapterButRetainsRemainingChapterAndOtherModules（重建Removes已撤回章节ButRetains剩余章节及OtherModules）。
 */

package com.starrainnotes.search;
import static org.mockito.Mockito.*;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.api.*;
import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.service.impl.SearchRebuildServiceImpl;
import com.starrainnotes.search.utils.SearchTextExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 教程公开版本搜索测试，属于搜索模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialPublicVersionSearchTest {
    /**
     * 重建Removes已撤回章节ButRetains剩余章节及OtherModules。
     * 关键处理步骤调用 <code>List.of</code>、<code>tutorials.page</code>、<code>PageResult.builder</code>、<code>mapper.activeKeys</code>。
     */
    @Test void rebuildRemovesWithdrawnChapterButRetainsRemainingChapterAndOtherModules() {
        var tutorials=mock(TutorialDocumentSourceApi.class);var index=mock(SearchIndexApi.class);var mapper=mock(SearchDocumentMapper.class);
        var chapter=new TutorialChapterDocument(5L,"two","Public two","Summary","Body","Body");
        var tutorial=new TutorialPublishedDocument(1L,"java","Java","Summary",null,List.of(chapter));
        when(tutorials.page(1,100)).thenReturn(PageResult.<TutorialPublishedDocument>builder().items(List.of(tutorial)).total(1L).page(1).pageSize(100).build());
        when(mapper.activeKeys("TUTORIAL")).thenReturn(List.of("TUTORIAL:1"));
        when(mapper.activeKeys("CHAPTER")).thenReturn(List.of("CHAPTER:4","CHAPTER:5"));
        var rebuild=new SearchRebuildServiceImpl(null,tutorials,null,null,index,mapper,new SearchTextExtractor(),null);
        rebuild.rebuildType("TUTORIAL");
        verify(index).remove("CHAPTER:4");verify(index,never()).remove("CHAPTER:5");
        verify(index).upsert(argThat(doc -> "CHAPTER:5".equals(doc.getDocumentKey()) && "/tutorials/java/two".equals(doc.getRoutePath())));
        verify(mapper,never()).activeKeys("BLOG");verify(mapper,never()).activeKeys("PORTFOLIO");
    }
}
