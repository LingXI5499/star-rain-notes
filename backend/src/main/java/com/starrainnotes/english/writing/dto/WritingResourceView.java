package com.starrainnotes.english.writing.dto;
import java.util.List;
public record WritingResourceView(Long id,String resourceKind,String expressionLevel,String title,String slug,String summary,String bodyMarkdown,
 Long coverMediaId,String coverUrl,String cefrLevel,Integer wordMin,Integer wordMax,Integer estimatedMinutes,String templateSchemaJson,
 String publishStatus,Integer sortOrder,String publishedAt,String updatedAt,List<WritingTagRef> tags) {}
