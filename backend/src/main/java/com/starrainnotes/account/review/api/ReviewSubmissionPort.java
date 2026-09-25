package com.starrainnotes.account.review.api;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;

/** Submit and publication checks used by content modules. */
public interface ReviewSubmissionPort {
    boolean isPublished(String contentType, long contentId);

    ContentReviewView submitBlogUpdate(Long actorId, Long postId, UpdatePostRequest request);

    ContentReviewView submitEnglishUpdate(Long actorId, String contentType, Long contentId, String title, Object request);

    ContentReviewView submitTutorialChapterUpdate(Long actorId, Long tutorialId, Long chapterId, UpdateChapterRequest request);
}
