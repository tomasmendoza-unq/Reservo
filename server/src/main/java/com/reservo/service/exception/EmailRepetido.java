package com.reservo.service.exception;

import org.springframework.http.HttpStatus;

public class EmailRepetido extends BusinessException {
    public EmailRepetido(String message) {
        super(HttpStatus.BAD_REQUEST, "DUPLICATE_EMAIL", message);
    }
}