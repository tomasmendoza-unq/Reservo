package com.reservo.service.exception.user;

import com.reservo.service.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UsuarioNoExiste extends BusinessException {
    public UsuarioNoExiste(String message) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", message);
    }
}
