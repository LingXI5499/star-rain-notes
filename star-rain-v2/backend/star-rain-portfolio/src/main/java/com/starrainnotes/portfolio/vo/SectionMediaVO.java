package com.starrainnotes.portfolio.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SectionMediaVO {
    private String mediaAssetId;
    private String caption;
    private String url;
    private String mediaType;
}
