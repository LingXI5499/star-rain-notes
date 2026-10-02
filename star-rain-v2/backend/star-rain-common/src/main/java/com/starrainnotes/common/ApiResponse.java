package com.starrainnotes.common;

public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data);
    }
}

