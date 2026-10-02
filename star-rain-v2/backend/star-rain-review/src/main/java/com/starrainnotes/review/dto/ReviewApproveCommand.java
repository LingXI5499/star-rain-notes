package com.starrainnotes.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * REV-004 审核通过入参。
 *
 * note 可选：通过时通常不需要解释，但允许 Reviewer 留一句审核备注，
 * 会写进 sr_review_action.note 供后续追溯。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewApproveCommand {

    @Size(max = 2000)
    private String note;
}
