package com.reservo.controller;

import com.reservo.controller.dto.Reservas.ReservaCanceladasRechazadasDTO;
import com.reservo.controller.dto.Reservas.ReservaPendienteDTO;
import com.reservo.controller.dto.Reservas.ReservaVigenteDTO;
import com.reservo.modelo.reserva.Peticion;
import com.reservo.service.PeticionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mis-reservas")
public final class ReservaControllerREST {
    private final PeticionService peticionService;

    public ReservaControllerREST(PeticionService peticionService) {
        this.peticionService = peticionService;
    }

    @GetMapping("/pendientes/{id}")
    public ResponseEntity<Page<ReservaPendienteDTO>> findAllReservasPendientesByUserId
            (@PathVariable Long id,
             @RequestParam(defaultValue = "0") int page){
        Page<Peticion> reservaPage = peticionService.findAllReservasPendientesByUserId(id, PageRequest.of(page, 10));

        if(reservaPage.isEmpty()) return ResponseEntity.status(404).body(null);

        Page<ReservaPendienteDTO> reservas = reservaPage.map(ReservaPendienteDTO::desdeModelo);

        return ResponseEntity.ok(reservas);
    }
    @GetMapping("/aceptadas/{id}")
    public ResponseEntity<Page<ReservaVigenteDTO>> findAllReservasVigentesByUserId
            (@PathVariable Long id,
             @RequestParam(defaultValue = "0") int page){
        Page<Peticion> reservaPage = peticionService.findAllReservasVigentesByUserId(id, PageRequest.of(page, 10));

        if(reservaPage.isEmpty()) return ResponseEntity.status(404).body(null);

        Page<ReservaVigenteDTO> reservas = reservaPage.map(ReservaVigenteDTO::desdeModelo);

        return ResponseEntity.ok(reservas);
    }

    @GetMapping("/canceladas/{id}")
    public ResponseEntity<Page<ReservaCanceladasRechazadasDTO>> findAllReservasCanceladasByUserId
            (@PathVariable Long id,
             @RequestParam(defaultValue = "0") int page){
        Page<Peticion> reservaPage = peticionService.findAllReservasCanceladasByUserId(id, PageRequest.of(page, 10));

        if(reservaPage.isEmpty()) return ResponseEntity.status(404).body(null);

        Page<ReservaCanceladasRechazadasDTO> reservas = reservaPage.map(ReservaCanceladasRechazadasDTO::desdeModelo);

        return ResponseEntity.ok(reservas);
    }
}
