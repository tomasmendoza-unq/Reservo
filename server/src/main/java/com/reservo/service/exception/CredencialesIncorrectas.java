package com.reservo.service.exception;

import org.springframework.http.HttpStatus;

public class CredencialesIncorrectas extends BusinessException {
    public CredencialesIncorrectas(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}