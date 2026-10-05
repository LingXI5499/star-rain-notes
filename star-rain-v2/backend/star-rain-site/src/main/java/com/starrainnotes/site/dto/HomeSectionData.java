package com.starrainnotes.site.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HomeSectionData {
    private String code;
    private String title;
    private Object data;
    private String status;
    private String layout;
}
