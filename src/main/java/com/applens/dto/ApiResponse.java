package com.applens.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String status;
    private final T data;
    private final String message;
    private final Integer code;
    private final Instant timestamp;

    private ApiResponse(String status, T data, String message, Integer code) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = code;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null, null);
    }

    public static <T> ApiResponse<T> error(String message, Integer code) {
        return new ApiResponse<>("error", null, message, code);
    }
}
