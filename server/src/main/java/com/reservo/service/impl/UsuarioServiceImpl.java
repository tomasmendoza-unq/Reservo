package com.reservo.service.impl;

import com.reservo.controller.dto.Usuario.CredentialsDTO;
import com.reservo.controller.exception.ParametroIncorrecto;
import com.reservo.modelo.property.Inmueble;
import com.reservo.modelo.user.AuthInfo;
import com.reservo.modelo.user.Credentials;
import com.reservo.modelo.user.Usuario;
import com.reservo.persistencia.DAO.PeticionDAO;
import com.reservo.persistencia.DAO.inmueble.InmuebleDAO;
import com.reservo.persistencia.DAO.user.AuthInfoDAO;
import com.reservo.persistencia.DAO.user.UsuarioDAO;
import com.reservo.service.UsuarioService;
import com.reservo.service.exception.CredencialesIncorrectas;
import com.reservo.service.exception.EmailRepetido;
import com.reservo.service.exception.user.UsuarioNoExiste;
import com.reservo.service.exception.user.UsuarioNoPuedeSerEliminado;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final AuthInfoDAO authInfoDAO;
    private final PeticionDAO peticionDAO;
    private final InmuebleDAO inmuebleDAO;

    public UsuarioServiceImpl(UsuarioDAO usuarioDAO, AuthInfoDAO authInfoDAO, PeticionDAO peticionDAO, InmuebleDAO inmuebleDAO) {
        this.usuarioDAO = usuarioDAO;
        this.authInfoDAO = authInfoDAO;
        this.peticionDAO = peticionDAO;
        this.inmuebleDAO = inmuebleDAO;
    }

    @Override
    public Usuario create(Usuario usuario) throws EmailRepetido {
        if (usuarioDAO.existeEmail(usuario.getEmail(), usuario.getId())) throw new EmailRepetido("El email ya se encuentra registrado.");
        return usuarioDAO.save(usuario);
    }

    @Override
    public Optional<Usuario> findById(Long userId) {
        return usuarioDAO.findById(userId);
    }

    @Override
    public List<Usuario> findAll() {
        return usuarioDAO.findAll();
    }

    @Override
    public void update(Usuario usuario) {
        if (usuario.getId() == null) throw new IllegalArgumentException("El usuario debe tener un ID para poder actualizarse.");

        if (usuarioDAO.findById(usuario.getId()).isEmpty())
            throw new EntityNotFoundException("Usuario no encontrado");

        usuarioDAO.save(usuario);
    }

    @Override
    public void delete(Long userId) {
        Optional<Usuario> usuario = usuarioDAO.findById(userId);
        if (usuario.isEmpty()) throw new UsuarioNoExiste("No existe el usuario que quiere eliminar.");

        boolean tieneReservasVigentes = usuarioDAO.tieneReservasVigentes(userId);
        boolean peticionesVigentes = usuarioDAO.tienePeticionesVigentes(userId);

        if (peticionesVigentes && tieneReservasVigentes) throw new UsuarioNoPuedeSerEliminado("No se puede eliminar la cuenta porque tiene reservas y peticiones en proceso.");
        if (tieneReservasVigentes) throw new UsuarioNoPuedeSerEliminado("No se puede eliminar la cuenta porque tiene reservas en proceso.");
        if (peticionesVigentes) throw new UsuarioNoPuedeSerEliminado("No se puede eliminar la cuenta porque tiene peticiones de sus inmuebles, todavía en proceso.");

        peticionDAO.deleteByClient(userId);
        peticionDAO.deleteByOwner(userId);
        inmuebleDAO.deleteByOwner(userId);
        authInfoDAO.deleteByUserId(userId);
        usuarioDAO.deleteById(userId);
    }

    @Override
    public CredentialsDTO login(Credentials credentials) throws CredencialesIncorrectas {
        Usuario user = usuarioDAO.getUsuarioConCredenciales(credentials.email(), credentials.password()).orElseThrow(() -> new CredencialesIncorrectas("Las credenciales dadas son erróneas."));

        removePreviousKey(user);

        String username = user.getName();

        AuthInfo authInfo = authInfoDAO.save(new AuthInfo(user));

        return (new CredentialsDTO(user.getId(),authInfo.getId(), username));
    }

    @Override
    public void emailRepetido(String email, Long idActual) throws EmailRepetido{

        if (usuarioDAO.existeEmail(email, idActual)) {
            throw new EmailRepetido("El email ya está registrado por otro usuario.");
        }

    }

    private void removePreviousKey(Usuario user) {
        Optional<AuthInfo> infoDeUsuario = authInfoDAO.getInfoDeUsuario(user.getId());
        infoDeUsuario.ifPresent(authInfoDAO::delete);
        authInfoDAO.flush();
    }

}
