package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class RangoDeHorarioSuperado extends BusinessException {
    public RangoDeHorarioSuperado() {
        super(HttpStatus.BAD_REQUEST, "SCHEDULE_RANGE_EXCEEDED", "Se supero el rango del horario");
    }

    public RangoDeHorarioSuperado(String message) {
        super(HttpStatus.BAD_REQUEST, "SCHEDULE_RANGE_EXCEEDED", message);
    }
}
