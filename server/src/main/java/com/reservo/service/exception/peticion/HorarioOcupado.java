package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class HorarioOcupado extends BusinessException {
    public HorarioOcupado(String message) {
        super(HttpStatus.CONFLICT, "SCHEDULE_OCCUPIED", message);
    }
}
