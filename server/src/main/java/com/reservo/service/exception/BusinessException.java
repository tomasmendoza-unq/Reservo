package com.reservo.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base para todas las excepciones de regla de negocio.
 * Encapsula el código HTTP, tipo de error y mensaje.
 */
public class BusinessException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public BusinessException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BusinessException(HttpStatus httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
