package com.reservo.controller.exception;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ParametroIncorrecto extends BusinessException {
    public ParametroIncorrecto(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
