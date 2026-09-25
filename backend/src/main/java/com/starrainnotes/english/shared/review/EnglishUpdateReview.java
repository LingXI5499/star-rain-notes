package com.starrainnotes.english.shared.review;

import com.starrainnotes.account.review.api.ReviewSubmissionPort;
import com.starrainnotes.account.review.api.ReviewSubmissionResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/** Published English edits by a non-super-admin become a review request. */
@Component
public class EnglishUpdateReview {
    private final ObjectProvider<ReviewSubmissionPort> reviews;

    public EnglishUpdateReview(ObjectProvider<ReviewSubmissionPort> reviews) {
        this.reviews = reviews;
    }

    public ReviewSubmissionResult decide(boolean superAdmin, String contentType, long contentId, String title,
                                         Object request, Long actorId, Supplier<Object> apply) {
        ReviewSubmissionPort port = reviews.getObject();
        if (!superAdmin && port.isPublished(contentType, contentId)) {
            return new ReviewSubmissionResult(true,
                    port.submitEnglishUpdate(actorId, contentType, contentId, title, request));
        }
        return new ReviewSubmissionResult(false, apply.get());
    }
}
