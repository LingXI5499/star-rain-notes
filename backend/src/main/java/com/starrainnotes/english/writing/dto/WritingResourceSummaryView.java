package com.starrainnotes.english.writing.dto;
import java.util.List;
public record WritingResourceSummaryView(Long id,String resourceKind,String expressionLevel,String title,String slug,String summary,String coverUrl,String cefrLevel,Integer estimatedMinutes,String publishStatus,Integer sortOrder,String updatedAt,List<WritingTagRef> tags) {
 public WritingResourceSummaryView withTags(List<WritingTagRef> value){return new WritingResourceSummaryView(id,resourceKind,expressionLevel,title,slug,summary,coverUrl,cefrLevel,estimatedMinutes,publishStatus,sortOrder,updatedAt,value);}
}
