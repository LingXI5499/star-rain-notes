package com.starrainnotes.tutorial.content.vo;

import lombok.Data;

/** Lightweight complete category order, requested only in explicit sorting mode. */
@Data
public class TutorialOrderItemVO {
    private String id;
    private String title;
    private Integer sortOrder;
}
