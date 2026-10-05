package com.starrainnotes.english.writing.service;

import com.starrainnotes.english.writing.dto.WritingResourceDto.Page;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Request;
import com.starrainnotes.english.writing.dto.WritingResourceDto.Resource;

// 写作素材读写入口。
public interface WritingResourceService {

    Page list(boolean admin, String search, int page, int size);

    Resource get(String identity, boolean admin);

    Resource create(Request request);

    Resource update(String identity, Request request);

    Resource setPublished(String identity, boolean published);

    void delete(String identity);
}
