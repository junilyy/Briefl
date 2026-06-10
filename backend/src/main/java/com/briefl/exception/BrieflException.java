package com.briefl.exception;

import lombok.Getter;

@Getter
public class BrieflException extends RuntimeException {

    private final ErrorCode errorCode;

    public BrieflException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BrieflException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
