package com.cp.friend.exception;

public record ValidationError(
        String field,
        String message
) {
}
