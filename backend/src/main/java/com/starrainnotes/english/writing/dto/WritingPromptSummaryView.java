package com.starrainnotes.english.writing.dto;
import java.util.List;
public record WritingPromptSummaryView(Long id,String title,String slug,String summary,String cefrLevel,Integer wordMin,Integer wordMax,Integer estimatedMinutes,String coverUrl,String publishStatus,Integer sortOrder,String updatedAt,List<WritingTagRef> tags) {
 public WritingPromptSummaryView withTags(List<WritingTagRef> value){return new WritingPromptSummaryView(id,title,slug,summary,cefrLevel,wordMin,wordMax,estimatedMinutes,coverUrl,publishStatus,sortOrder,updatedAt,value);}
}
