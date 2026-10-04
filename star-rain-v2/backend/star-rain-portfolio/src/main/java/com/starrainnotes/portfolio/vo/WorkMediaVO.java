package com.starrainnotes.portfolio.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkMediaVO {
    private String id;
    private String mediaAssetId;
    private String usageType;
    private String caption;
    private int sortOrder;
    private String url;
}
