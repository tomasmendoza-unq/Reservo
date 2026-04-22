package com.reservo.service.exception;

import org.springframework.http.HttpStatus;

public class InmuebleRepetidoException extends BusinessException {
    public InmuebleRepetidoException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_PROPERTY", message);
    }
}
