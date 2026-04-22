package com.reservo.controller;

import com.reservo.controller.dto.Usuario.*;
import com.reservo.controller.exception.ResponseErrorDTO;
import com.reservo.controller.exception.ParametroIncorrecto;
import com.reservo.modelo.user.Credentials;
import com.reservo.modelo.user.Usuario;
import com.reservo.service.UsuarioService;
import com.reservo.service.exception.CredencialesIncorrectas;
import com.reservo.service.exception.EmailRepetido;
import com.reservo.service.exception.user.UsuarioNoExiste;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public final class UsuarioControllerREST {
    private final UsuarioService usuarioService;

    public UsuarioControllerREST(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> createUsuario(@RequestBody UsuarioRequestDTO usuarioRequestDTO) throws ParametroIncorrecto, EmailRepetido {
        Usuario user = this.usuarioService.create(usuarioRequestDTO.aModelo());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.desdeModelo(user));
    }

    @PostMapping("/login/")
    public ResponseEntity<CredentialsDTO> loginById(@RequestBody LoginRequestDTO loginRequestDTO) throws CredencialesIncorrectas {

        Credentials userCredentials = new Credentials(loginRequestDTO.email(), loginRequestDTO.password());

        CredentialsDTO loginCredentials = usuarioService.login(userCredentials);
        if (loginCredentials == null) return ResponseEntity.status(404).body(null);
        return ResponseEntity.ok(loginCredentials);
    }

    @GetMapping
    public ResponseEntity<Set<UsuarioResponseDTO>> getAllUsuarios() {
        return ResponseEntity.ok(this.usuarioService.findAll().stream()
                .map(UsuarioResponseDTO::desdeModelo)
                .collect(Collectors.toSet()));
    }

    @GetMapping("/datos-usuario/{id}")
    public ResponseEntity<UsuarioResponseDTO> getUsuarioById(@PathVariable Long id) {
        if(this.usuarioService.findById(id).isEmpty()) return ResponseEntity.status(404).body(null);
        Usuario usuario = this.usuarioService.findById(id).get();
        return ResponseEntity.ok(UsuarioResponseDTO.desdeModelo(usuario));
    }

    @PutMapping("/modifyUserName/{id}")
    public ResponseEntity<ResponseErrorDTO> modifyUserName(@PathVariable Long id,
                                                           @RequestBody CampoActualizadoDTO valorDTO) {
        Optional<Usuario> optUsuario = usuarioService.findById(id);
        if (optUsuario.isEmpty()) throw new UsuarioNoExiste("No existe el usuario que quiere modificar.");

        Usuario usuario = optUsuario.get();
        //agregar cualquier validacion extra para contraseña

        usuario.setName(valorDTO.valor());

        usuarioService.update(usuario);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/modifyUserEmail/{id}")
    public ResponseEntity<ResponseErrorDTO> modifyUserEmail(@PathVariable Long id,
                                                            @RequestBody CampoActualizadoDTO valorDTO) throws EmailRepetido {
        Optional<Usuario> optUsuario = usuarioService.findById(id);
        if (optUsuario.isEmpty()) throw new UsuarioNoExiste("No existe el usuario que quiere modificar.");

        Usuario usuario = optUsuario.get();

        usuarioService.emailRepetido(valorDTO.valor(), usuario.getId());

        //agregar cualquier validacion extra para contraseña

        usuario.setEmail(valorDTO.valor());

        usuarioService.update(usuario);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/modifyUserPassword/{id}")
    public ResponseEntity<ResponseErrorDTO> modifyUserPassword(@PathVariable Long id,
                                                               @RequestBody CampoActualizadoDTO valorDTO) {
        Optional<Usuario> optUsuario = usuarioService.findById(id);
        if (optUsuario.isEmpty()) throw new UsuarioNoExiste("No existe el usuario que quiere modificar.");

        Usuario usuario = optUsuario.get();
        //agregar cualquier validacion extra para contraseña

        usuario.setPassword(valorDTO.valor());

        usuarioService.update(usuario);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseErrorDTO> deleteUser(@PathVariable Long id) {
        usuarioService.delete(id);
        return ResponseEntity.ok().build();
    }

}

