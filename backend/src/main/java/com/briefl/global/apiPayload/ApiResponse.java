package com.briefl.global.apiPayload;

public record ApiResponse<T>(
        boolean isSuccess,
        String code,
        String message,
        T result
) {

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(true, "COMMON200", "요청이 성공했습니다.", result);
    }

    public static <T> ApiResponse<T> failure(String code, String message, T result) {
        return new ApiResponse<>(false, code, message, result);
    }
}
