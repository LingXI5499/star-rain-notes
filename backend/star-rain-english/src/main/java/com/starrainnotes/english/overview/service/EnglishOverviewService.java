package com.starrainnotes.english.overview.service;

import com.starrainnotes.english.overview.dto.EnglishOverviewRequestDTO;
import com.starrainnotes.english.overview.vo.EnglishOverviewVO;

// 英语模块总览内容读写入口。
public interface EnglishOverviewService {

    EnglishOverviewVO get();

    EnglishOverviewVO update(EnglishOverviewRequestDTO request);
}
