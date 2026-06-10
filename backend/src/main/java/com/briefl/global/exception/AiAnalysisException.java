package com.briefl.global.exception;

public class AiAnalysisException extends BrieflException {

    public AiAnalysisException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AiAnalysisException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
