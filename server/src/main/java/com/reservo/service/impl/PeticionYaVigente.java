package com.reservo.service.impl;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PeticionYaVigente extends BusinessException {
    public PeticionYaVigente(String message) {
        super(HttpStatus.CONFLICT, "PETITION_ALREADY_ACTIVE", message);
    }
}
