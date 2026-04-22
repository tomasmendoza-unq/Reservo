package com.reservo.service.exception;

import org.springframework.http.HttpStatus;

public class TienePeticionVigenteException extends BusinessException {
    public TienePeticionVigenteException(String message) {
        super(HttpStatus.CONFLICT, "HAS_ACTIVE_PETITION", message);
    }
}
