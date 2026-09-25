package com.starrainnotes.account.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.audit.AuditLogService;
import com.starrainnotes.account.review.api.ReviewSubmissionPort;
import com.starrainnotes.account.review.api.ReviewedContentHandler;
import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.infrastructure.ContentReviewRepository;
import com.starrainnotes.blog.dto.UpdatePostRequest;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.api.EnglishReviewContentPort;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ContentReviewService implements ReviewSubmissionPort {
    private final ContentReviewRepository reviews;
    private final ObjectMapper json;
    private final List<ReviewedContentHandler> handlers;
    private final AuditLogService auditLogService;
    private final EnglishReviewContentPort englishContent;

    public ContentReviewService(ContentReviewRepository reviews, ObjectMapper json,
                                List<ReviewedContentHandler> handlers, AuditLogService auditLogService,
                                EnglishReviewContentPort englishContent) {
        this.reviews = reviews;
        this.json = json;
        this.handlers = handlers;
        this.auditLogService = auditLogService;
        this.englishContent = englishContent;
    }

    public List<ContentReviewView> list(String status, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        return reviews.list(status, safePage, safeSize);
    }

    @Override
    @Transactional
    public ContentReviewView submitBlogUpdate(Long actorId, Long postId, UpdatePostRequest request) {
        return submit(actorId, "BLOG_POST", postId, request.title(), json.valueToTree(request).toString(),
                "这篇文章已有待审核版本，请等待超级管理员处理。");
    }

    @Override
    public boolean isPublished(String contentType, long contentId) {
        return englishContent.isPublished(contentType, contentId);
    }

    @Override
    @Transactional
    public ContentReviewView submitEnglishUpdate(Long actorId, String contentType, Long contentId,
                                                 String title, Object request) {
        return submit(actorId, contentType, contentId, title, json.valueToTree(request).toString(),
                "这条内容已有待审核版本，请等待超级管理员处理。");
    }

    @Override
    @Transactional
    public ContentReviewView submitTutorialChapterUpdate(Long actorId, Long tutorialId, Long chapterId,
                                                         UpdateChapterRequest request) {
        String payload = json.valueToTree(Map.of("tutorialId", tutorialId, "request", request)).toString();
        return submit(actorId, "TUTORIAL_CHAPTER", chapterId, request.title(), payload,
                "这个章节已有待审核版本，请等待超级管理员处理。");
    }

    @Transactional
    public ContentReviewView approve(Long id, Long reviewerId, String note) {
        ContentReviewView review = reviews.require(id);
        ensurePending(review);
        handlers.stream()
                .filter(handler -> handler.supports(review.contentType(), review.actionType()))
                .findFirst()
                .ifPresent(handler -> handler.apply(review.contentId(), review.payload()));
        reviews.markApproved(id, reviewerId, clean(note));
        auditLogService.record(reviewerId, "CONTENT_REVIEW_APPROVED", review.contentType(), review.contentId(), "SUCCESS", null, null,
                Map.of("reviewId", id, "action", review.actionType()));
        return reviews.require(id);
    }

    @Transactional
    public ContentReviewView reject(Long id, Long reviewerId, String note) {
        ContentReviewView review = reviews.require(id);
        ensurePending(review);
        reviews.markRejected(id, reviewerId, clean(note));
        auditLogService.record(reviewerId, "CONTENT_REVIEW_REJECTED", review.contentType(), review.contentId(), "SUCCESS", null, null,
                Map.of("reviewId", id, "action", review.actionType()));
        return reviews.require(id);
    }

    private ContentReviewView submit(Long actorId, String contentType, long contentId, String title,
                                     String payloadJson, String pendingDetail) {
        if (reviews.countPending(contentType, contentId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REVIEW_PENDING",
                    "Review already pending", pendingDetail);
        }
        long id = reviews.insert(contentType, contentId, title, payloadJson, actorId);
        auditLogService.record(actorId, "CONTENT_REVIEW_SUBMITTED", contentType, contentId, "SUCCESS", null, null,
                Map.of("reviewId", id, "action", "UPDATE"));
        return reviews.require(id);
    }

    private void ensurePending(ContentReviewView review) {
        if (!"PENDING".equals(review.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REVIEW_CLOSED",
                    "Review already closed", "该审核已处理，不能重复操作。");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
