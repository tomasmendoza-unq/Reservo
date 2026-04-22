package com.reservo.service.impl;

import com.reservo.controller.dto.Inmueble.InmuebleModifyRequestDTO;
import com.reservo.controller.dto.Inmueble.InmuebleRemoveImagesDTO;
import com.reservo.controller.exception.ParametroIncorrecto;
import com.reservo.modelo.Filtro;
import com.reservo.modelo.property.Inmueble;
import com.reservo.modelo.property.ReservoImage;
import com.reservo.persistencia.DAO.PeticionDAO;
import com.reservo.persistencia.DAO.inmueble.InmuebleDAO;
import com.reservo.service.ImageService;
import com.reservo.service.InmuebleService;
import com.reservo.service.exception.InmuebleRepetidoException;
import com.reservo.service.exception.NoExisteInmuebleExpcetion;
import com.reservo.service.exception.TienePeticionVigenteException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Transactional
public class InmuebleServiceImpl implements InmuebleService {

    private final InmuebleDAO inmuebleDAO;
    private final ImageService imageService;
    private final PeticionDAO peticionDAO;

    public InmuebleServiceImpl(InmuebleDAO dao, ImageService imageService, PeticionDAO peticionDAO) {
        this.inmuebleDAO = dao;
        this.imageService = imageService;
        this.peticionDAO = peticionDAO;
    }

    @Override
    public Inmueble create(Inmueble inmueble, List<MultipartFile> images) {
        List<ReservoImage> savedImages = imageService.saveImages(images);
        inmueble.getImages().addAll(savedImages);

        if (inmuebleDAO.existeInmueble(inmueble.getId())) {
            throw new InmuebleRepetidoException("El inmueble ya está registrado.");
        }
        return inmuebleDAO.save(inmueble);
    }

    @Override
    public void delete(Long inmuebleId) {
        if (!inmuebleDAO.existeInmueble(inmuebleId))
            throw new NoExisteInmuebleExpcetion("No existe la publicación que quiere eliminar");

        if (inmuebleDAO.tienePeticionesVigentes(inmuebleId))
            throw new TienePeticionVigenteException("El inmueble tiene peticiones vigentes todavía");

        peticionDAO.deleteByInmueble(inmuebleId);
        inmuebleDAO.deleteById(inmuebleId);
    }

    @Override
    public Optional<Inmueble> findById(Long inmuebleId) {
        return inmuebleDAO.findById(inmuebleId);
    }

    @Override
    public List<Inmueble> findAll() {
        return inmuebleDAO.findAll();
    }

    @Override
    public Page<Inmueble> findByFiltro(Filtro filtro) {
        return inmuebleDAO.findByFiltros(filtro.getNombre(),
                filtro.getLocalidad(),
                filtro.getPrecioMin(),
                filtro.getPrecioMax(),
                filtro.getHorarioMin(),
                filtro.getHorarioMax(),
                filtro.getCapacidad(),
                filtro.getPage());
    }

    @Override
    public Page<Inmueble> getAllByOwnerId(Long id, Pageable pageable) {
        return inmuebleDAO.getAllByOwnerId(id, pageable);
    }



    @Override
    public void update(Long inmuebleId, InmuebleModifyRequestDTO inmuebleDTO) throws ParametroIncorrecto {
        if (!inmuebleDAO.existeInmueble(inmuebleId))
            throw new NoExisteInmuebleExpcetion("No existe la publicación que quiere modificar");

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElseThrow(() -> new ParametroIncorrecto("El inmueble no existe."));
        Inmueble inmuebleModificado = inmuebleDTO.aModeloModificado(inmueble);

        inmuebleDAO.save(inmuebleModificado);
    }

    @Override
    public void addImages(Long inmuebleId, List<MultipartFile> images) throws ParametroIncorrecto {
        if (!inmuebleDAO.existeInmueble(inmuebleId)) {
            throw new NoExisteInmuebleExpcetion("Ya no existe el inmueble al que se quiere acceder");
        }

        if (images.isEmpty()) {
            return;
        }

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId)
                .orElseThrow(() -> new ParametroIncorrecto("El inmueble no existe."));
        List<ReservoImage> savedImages = imageService.saveImages(images);
        inmueble.getImages().addAll(savedImages);

        inmuebleDAO.save(inmueble);
    }

    @Override
    public void removeImages(Long inmuebleId, InmuebleRemoveImagesDTO images) throws ParametroIncorrecto {
        if (!inmuebleDAO.existeInmueble(inmuebleId)) {
            throw new NoExisteInmuebleExpcetion("Ya no existe el inmueble al que se quiere acceder");
        }

        List<Integer> imagesToRemove = images.imagesToRemove();
        if (imagesToRemove.isEmpty()) {
            return;
        }

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId)
                .orElseThrow(() -> new ParametroIncorrecto("El inmueble no existe."));

        List<ReservoImage> imagesToDelete = new ArrayList<>();
        for (Integer index : imagesToRemove) {
            imagesToDelete.add(inmueble.getImages().get(index));
        }

        imageService.deleteImages(imagesToDelete);
        imagesToDelete.forEach(inmueble.getImages()::remove);

        inmuebleDAO.save(inmueble);
    }


}
