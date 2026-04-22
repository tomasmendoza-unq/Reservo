package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class RealizoUnaPeticionSobreElInmuebleEnElMismoDia extends BusinessException {

    public RealizoUnaPeticionSobreElInmuebleEnElMismoDia() {
        super(HttpStatus.CONFLICT, "PETITION_SAME_DAY", "Sólo se admite una reserva por día.");
    }

    public RealizoUnaPeticionSobreElInmuebleEnElMismoDia(String message) {
        super(HttpStatus.CONFLICT, "PETITION_SAME_DAY", message);
    }
}
