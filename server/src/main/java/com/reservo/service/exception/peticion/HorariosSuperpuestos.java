package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class HorariosSuperpuestos extends BusinessException {
    public HorariosSuperpuestos() {
        super(HttpStatus.BAD_REQUEST, "OVERLAPPING_SCHEDULES", "Se solapan los horarios");
    }

    public HorariosSuperpuestos(String message) {
        super(HttpStatus.BAD_REQUEST, "OVERLAPPING_SCHEDULES", message);
    }
}
