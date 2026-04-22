package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PeticionVencida extends BusinessException {
    public PeticionVencida(String message) {
        super(HttpStatus.GONE, "EXPIRED_PETITION", message);
    }
}
