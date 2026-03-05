package com.musinsa.point.exception;

public record ErrorResponse(
        int status,
        String message,
        String code
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getMessage(),
                errorCode.getCode()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                message,
                errorCode.getCode()
        );
    }
}
