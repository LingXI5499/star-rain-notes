package com.starrainnotes.site.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HomeSectionVO {
    private String code;
    private String title;
    private Object data;
    private String status;
    private String layout;
}
