package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class VieneDelPasado extends BusinessException {
    public VieneDelPasado() {
        super(HttpStatus.BAD_REQUEST, "INVALID_DATE", "La fecha no puede ser del pasado");
    }

    public VieneDelPasado(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_DATE", message);
    }
}
