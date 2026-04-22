package com.reservo.controller;

import com.reservo.controller.dto.Peticion.*;

import com.reservo.controller.exception.ParametroIncorrecto;
import com.reservo.modelo.property.Inmueble;
import com.reservo.modelo.property.ReservoImage;
import com.reservo.modelo.reserva.Peticion;
import com.reservo.modelo.user.Usuario;
import com.reservo.service.InmuebleService;
import com.reservo.service.PeticionService;
import com.reservo.service.UsuarioService;
import com.reservo.service.exception.NoExisteInmuebleExpcetion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/peticion")
public class PeticionControllerREST {
    private final PeticionService peticionService;
    private final UsuarioService usuarioService;
    private final InmuebleService inmuebleService;

    public PeticionControllerREST(PeticionService peticionService,UsuarioService usuarioService,InmuebleService inmuebleService) {
        this.peticionService = peticionService;
        this.usuarioService = usuarioService;
        this.inmuebleService = inmuebleService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<PeticionResponseDTO> createPeticion(@RequestBody PeticionRequestDTO peticionDTO) throws ParametroIncorrecto {//TODO cambiar error, solo es temporal

        Usuario user = usuarioService.findById(peticionDTO.userId())
                .orElseThrow(() -> new ParametroIncorrecto("Usuario no encontrado"));

        Inmueble inmueble = inmuebleService.findById(peticionDTO.inmuebleId())
                .orElseThrow(() -> new NoExisteInmuebleExpcetion("No existe la publicación que quiere solicitar"));
        Peticion peticion = peticionDTO.aModelo(inmueble,user);

        Peticion saved = peticionService.create(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(PeticionResponseDTO.desdeModelo(saved));
    }

    @GetMapping("/vigentes/{inmuebleId}/{date}")
    public ResponseEntity<List<HorarioPeticionDTO>> findAllVigentesByDateInInmueble(@PathVariable Long inmuebleId, @PathVariable LocalDate date){

        List<Peticion> peticiones = peticionService.findAllVigentesByDateInInmueble(inmuebleId, date);
        List<HorarioPeticionDTO> horarios = peticiones.stream().map(HorarioPeticionDTO::desdeModelo).toList();

        return ResponseEntity.ok(horarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeticionResponseDTO> findPeticionById(@PathVariable Long id){
        if (this.peticionService.findById(id).isEmpty()) return ResponseEntity.status(404).body(null);
        Peticion peticion = peticionService.findById(id).get();
        return ResponseEntity.ok(PeticionResponseDTO.desdeModelo(peticion));
    }

    @GetMapping("/owner/pendiente/{id}")
    public ResponseEntity<Page<PeticionSummaryDTO>> findAllPendientByOwnerId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Peticion> findById = this.peticionService.findAllPendientByOwnerId(id, PageRequest.of(page, 10));

        if (findById.isEmpty()) return ResponseEntity.status(404).body(null);

        Page<PeticionSummaryDTO> peticiones = findById.map(PeticionSummaryDTO::desdeModelo);

        return ResponseEntity.ok(peticiones);
    }

    @GetMapping("/owner/aceptadas/{id}")
    public ResponseEntity<Page<PeticionSummaryDTO>> findAllApproveByOwnerId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Peticion> findById = this.peticionService.findAllApproveByOwnerId(id, PageRequest.of(page, 10));

        if (findById.isEmpty()) return ResponseEntity.status(404).body(null);

        Page<PeticionSummaryDTO> peticiones = findById.map(PeticionSummaryDTO::desdeModelo);

        return ResponseEntity.ok(peticiones);
    }

    @GetMapping("/owner/canceladas/{id}")
    public ResponseEntity<Page<PeticionSummaryDTO>> findAllRejectByOwnerId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Peticion> findById = this.peticionService.findAllRejectByOwnerId(id, PageRequest.of(page, 10));

        if (findById.isEmpty()) return ResponseEntity.status(404).body(null);

        Page<PeticionSummaryDTO> peticiones = findById.map(PeticionSummaryDTO::desdeModelo);

        return ResponseEntity.ok(peticiones);
    }


    @GetMapping("/pendiente/{id}")
    public ResponseEntity<PeticionPendienteResponseDTO> getPeticionById(@PathVariable Long id) {
        Optional<Peticion> peticion = this.peticionService.findById(id);

        return peticion.map(value -> ResponseEntity.ok(PeticionPendienteResponseDTO.desdeModelo(value)))
                .orElseGet(() -> ResponseEntity.status(404).body(null));
    }

    @GetMapping("/pendiente/{id}/images")
    public ResponseEntity<List<String>> getInmuebleImages(@PathVariable Long id) {
        Optional<Peticion> optionalPeticion = peticionService.findById(id);
        if (optionalPeticion.isEmpty()) {
            System.out.println("No existe la peticion con id " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Peticion pt = optionalPeticion.get();
        System.out.println("Peticion encontrada: " + pt.getId());

        if (pt.getInmueble() == null) {
            System.out.println("La peticion no tiene inmueble asociado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return inmuebleService.findById(pt.getInmueble().getId())
                .map(inmueble -> {
                    List<String> imagePaths = inmueble.getImages().stream().map(ReservoImage::getUrl).collect(Collectors.toList());
                    return ResponseEntity.ok(imagePaths);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PatchMapping("/aprobar")
    public ResponseEntity<Object> approve(@RequestBody AprobarDTO aprobarDTO){
        peticionService.approve(aprobarDTO.peticionId());

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/rechazar")
    public ResponseEntity<Object> reject(@RequestBody RechazoDTO rechazoDTO) {
        peticionService.reject(rechazoDTO);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/cancelar")
    public ResponseEntity<Object> cancel(@RequestBody CancelacionDTO cancelacionDTO) {
        peticionService.cancel(cancelacionDTO);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/declararPago/{peticionId}")
    public ResponseEntity<Object> declararPago(@PathVariable Long peticionId){
        peticionService.declararPago(peticionId);

        return ResponseEntity.ok().build();
    }

}
