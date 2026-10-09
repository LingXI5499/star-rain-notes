package com.starrainnotes.site.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotContentItemVO {
    private String type;
    private String title;
    private String path;
    private long viewCount;
}
