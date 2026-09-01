package com.starrainnotes.english.writing.dto;
import java.util.List;
public record WritingPageView<T>(List<T> items,int page,int pageSize,long total,int totalPages) {}
