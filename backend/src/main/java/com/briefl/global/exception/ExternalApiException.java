package com.briefl.global.exception;

public class ExternalApiException extends BrieflException {

    public ExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
