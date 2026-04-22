package com.reservo.service.exception.peticion;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class EsDueñoDeLaPropiedadSolicitada extends BusinessException {
    public EsDueñoDeLaPropiedadSolicitada() {
        super(HttpStatus.FORBIDDEN, "CANNOT_REQUEST_OWN_PROPERTY", "No puede solicitar su propia vivienda");
    }

    public EsDueñoDeLaPropiedadSolicitada(String message) {
        super(HttpStatus.FORBIDDEN, "CANNOT_REQUEST_OWN_PROPERTY", message);
    }
}
