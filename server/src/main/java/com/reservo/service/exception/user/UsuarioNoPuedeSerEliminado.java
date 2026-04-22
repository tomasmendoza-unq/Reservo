package com.reservo.service.exception.user;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UsuarioNoPuedeSerEliminado extends BusinessException {
    public UsuarioNoPuedeSerEliminado(String message) {
        super(HttpStatus.BAD_REQUEST, "USER_CANNOT_BE_DELETED", message);
    }
}
