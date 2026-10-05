package com.starrainnotes.english.reading.service;

import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingDto.Page;
import com.starrainnotes.english.reading.dto.ReadingDto.Request;

// 阅读理解材料读写入口。
public interface ReadingService {

    Page list(boolean admin, String search, int page, int size);

    Article get(String identity, boolean admin);

    Article create(Request request);

    Article update(String identity, Request request);

    Article setPublished(String identity, boolean published);

    void delete(String identity);
}
