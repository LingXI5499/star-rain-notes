package com.starrainnotes.tutorial.exception;

import com.starrainnotes.common.exception.ApiException;

public class TutorialGroupNotEmptyException extends ApiException {
    public TutorialGroupNotEmptyException() {
        super("TUTORIAL_GROUP_NOT_EMPTY", "分组仍有使用中的章节，请先移动或归档", 409);
    }
}
