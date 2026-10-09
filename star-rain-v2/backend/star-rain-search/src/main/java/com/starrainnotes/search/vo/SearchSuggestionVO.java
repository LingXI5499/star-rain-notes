package com.starrainnotes.search.vo;

import lombok.Data;

@Data
public class SearchSuggestionVO {
    private String text;
    private String contentType;
    private String routePath;
}
