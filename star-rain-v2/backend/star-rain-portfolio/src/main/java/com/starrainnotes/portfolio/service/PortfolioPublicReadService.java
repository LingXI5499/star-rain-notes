package com.starrainnotes.portfolio.service;

import com.starrainnotes.portfolio.vo.WorkVO;

// 作品集公开阅读入口：读取已发布作品详情并记录浏览事件。
public interface PortfolioPublicReadService {

    WorkVO read(String slug);
}
