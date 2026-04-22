package com.reservo.controller.dto.Usuario;

import com.reservo.controller.exception.ParametroIncorrecto;
import com.reservo.modelo.user.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record UsuarioRequestDTO(
        @NotNull(message = "El nombre debe no estar en blanco")
        @NotBlank(message = "El nombre debe no estar en blanco")
        String name,
        @NotNull(message = "La contraseña debe no estar en blanco")
        @NotBlank(message = "La contraseña debe no estar en blanco")
        String password,
        String email
) {

    public Usuario aModelo() throws ParametroIncorrecto {
        if (name == null || password == null || email == null) throw new ParametroIncorrecto("Faltan datos para registrar.");
        if (name.isBlank() ) throw new ParametroIncorrecto("El nombre no debe estar en blanco.");
        if (password.isBlank()) throw new ParametroIncorrecto("La contraseña no debe estar en blanco.");
        if (email.isBlank() ) throw new ParametroIncorrecto("El email no debe estar en blanco.");
        checkEmailFormat();

        return new Usuario(name, password, email);
    }

    private void checkEmailFormat() throws ParametroIncorrecto {
        String regex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        if(!matcher.matches()) throw new ParametroIncorrecto("El email debe ser de la forma example@email.com");
    }
}
