package com.starrainnotes.account.review.dto;

import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(@Size(max = 500) String note) {
}
