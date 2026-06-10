package com.briefl.domain.report.exception;

import com.briefl.global.exception.BrieflException;
import com.briefl.global.exception.ErrorCode;

public class ReportNotFoundException extends BrieflException {

    public ReportNotFoundException() {
        super(ErrorCode.REPORT_NOT_FOUND);
    }
}
