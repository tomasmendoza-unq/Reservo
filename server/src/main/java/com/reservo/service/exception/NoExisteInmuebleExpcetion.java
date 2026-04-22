package com.reservo.service.exception;

import org.springframework.http.HttpStatus;

public class NoExisteInmuebleExpcetion extends BusinessException {
    public NoExisteInmuebleExpcetion(String message) {
        super(HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND", message);
    }
}
