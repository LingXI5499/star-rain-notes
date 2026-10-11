package com.starrainnotes.tutorial.content.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialChapterNotFoundException extends ApiException {
    public TutorialChapterNotFoundException() {
        super("TUTORIAL_CHAPTER_NOT_FOUND", "教程章节不存在", 404);
    }
}
