package com.tablehub.operations.port;

import org.springframework.http.HttpStatus;

public class OperationsException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public OperationsException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
