package com.aiwechat.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommonResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> CommonResponse<T> success(T data) {
        CommonResponse<T> response = new CommonResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static <T> CommonResponse<T> success(T data, String message) {
        CommonResponse<T> response = success(data);
        response.setMessage(message);
        return response;
    }

    public static <T> CommonResponse<T> error(String error) {
        CommonResponse<T> response = new CommonResponse<>();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
}
