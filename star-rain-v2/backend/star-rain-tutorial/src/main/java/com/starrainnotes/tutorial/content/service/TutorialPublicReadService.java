package com.starrainnotes.tutorial.content.service;

import com.fasterxml.jackson.databind.JsonNode;

// 教程公开阅读入口：读取已发布的教程与章节并记录浏览事件。
public interface TutorialPublicReadService {

    JsonNode tutorial(String slug);

    JsonNode chapter(String tutorialSlug, String chapterSlug);
}
