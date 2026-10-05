package com.starrainnotes.site.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotContentItem {
    private String type;
    private String title;
    private String path;
    private long viewCount;
}
