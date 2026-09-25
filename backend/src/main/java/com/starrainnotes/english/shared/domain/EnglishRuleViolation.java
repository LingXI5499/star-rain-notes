package com.starrainnotes.english.shared.domain;

/**
 * English domain rule failure. HTTP status is chosen by {@code GlobalExceptionHandler}.
 */
public class EnglishRuleViolation extends RuntimeException {
    private final String code;
    private final String title;

    public EnglishRuleViolation(String code, String title, String detail) {
        super(detail);
        this.code = code;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
