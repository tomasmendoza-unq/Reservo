package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class HorarioDesordenado extends BusinessException {
    public HorarioDesordenado() {
        super(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE_ORDER", "Los horarios deben estar ordenados");
    }

    public HorarioDesordenado(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE_ORDER", message);
    }
}
