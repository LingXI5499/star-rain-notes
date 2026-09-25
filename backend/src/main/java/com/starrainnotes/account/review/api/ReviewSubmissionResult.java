package com.starrainnotes.account.review.api;

/** Either the updated content or the pending review view. HTTP status stays in the controller. */
public record ReviewSubmissionResult(boolean submitted, Object body) {
}
