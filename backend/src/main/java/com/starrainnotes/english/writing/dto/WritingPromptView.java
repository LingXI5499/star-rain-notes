package com.starrainnotes.english.writing.dto;
import java.util.List;
public record WritingPromptView(Long id,String title,String slug,String summary,String backgroundMarkdown,String requirementsMarkdown,String cefrLevel,Integer wordMin,Integer wordMax,Integer estimatedMinutes,String rubricJson,String checklistJson,Long templateResourceId,Long modelResourceId,Long coverMediaId,String coverUrl,String publishStatus,Integer sortOrder,String publishedAt,String updatedAt,List<WritingTagRef> tags) {}
