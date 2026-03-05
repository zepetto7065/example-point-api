package com.musinsa.point.exception;

import lombok.Getter;

@Getter
public class PointException extends RuntimeException {

    private final ErrorCode errorCode;

    public PointException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PointException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
