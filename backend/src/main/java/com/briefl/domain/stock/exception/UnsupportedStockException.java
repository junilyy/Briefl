package com.briefl.domain.stock.exception;

import com.briefl.global.exception.BrieflException;
import com.briefl.global.exception.ErrorCode;

public class UnsupportedStockException extends BrieflException {

    public UnsupportedStockException() {
        super(ErrorCode.UNSUPPORTED_STOCK);
    }
}
