package com.cp.friend.exception;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T errors
) {
}
