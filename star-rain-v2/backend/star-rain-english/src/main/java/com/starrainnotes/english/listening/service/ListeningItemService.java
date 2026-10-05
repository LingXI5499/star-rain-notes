package com.starrainnotes.english.listening.service;

import com.starrainnotes.english.listening.dto.ListeningItemDto.Item;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Page;
import com.starrainnotes.english.listening.dto.ListeningItemDto.Request;

// 听力材料读写入口。
public interface ListeningItemService {

    Page list(boolean admin, String search, int page, int size);

    Item get(String identity, boolean admin);

    Item create(Request request);

    Item update(String identity, Request request);

    Item setPublished(String identity, boolean published);

    void delete(String identity);
}
