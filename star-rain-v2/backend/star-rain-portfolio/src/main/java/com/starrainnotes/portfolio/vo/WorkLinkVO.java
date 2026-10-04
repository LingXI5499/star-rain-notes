package com.starrainnotes.portfolio.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLinkVO {
    private String id;
    private String linkType;
    private String label;
    private String url;
    private int sortOrder;
    private boolean enabled;
}
