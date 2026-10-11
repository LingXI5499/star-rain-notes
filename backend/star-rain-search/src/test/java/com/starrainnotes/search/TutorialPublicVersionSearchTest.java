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
class TutorialPublicVersionSearchTest {
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
