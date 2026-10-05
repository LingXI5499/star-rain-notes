package com.starrainnotes.english.writing.service;

import com.starrainnotes.english.writing.dto.WritingPromptDto.Page;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Prompt;
import com.starrainnotes.english.writing.dto.WritingPromptDto.Request;

// 写作题目读写入口。
public interface WritingPromptService {

    Page list(boolean admin, String search, int page, int size);

    Prompt get(String identity, boolean admin);

    Prompt create(Request request);

    Prompt update(String identity, Request request);

    Prompt setPublished(String identity, boolean published);

    void delete(String identity);
}
