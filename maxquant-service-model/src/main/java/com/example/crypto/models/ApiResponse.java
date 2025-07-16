package com.example.crypto.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一API响应格式
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建一个成功的响应
     * @param data 响应数据
     * @return ApiResponse 实例
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    /**
     * 创建一个不带数据的成功响应
     * @return ApiResponse 实例
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "Success", null);
    }

    /**
     * 创建一个失败的响应
     * @param code 错误码
     * @param message 错误信息
     * @return ApiResponse 实例
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
} 