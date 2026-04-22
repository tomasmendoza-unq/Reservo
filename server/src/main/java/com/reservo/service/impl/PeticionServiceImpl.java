package com.reservo.service.impl;

import com.reservo.controller.CancelacionDTO;
import com.reservo.controller.dto.Peticion.RechazoDTO;
import com.reservo.modelo.peticion.EmailMessages;
import com.reservo.modelo.property.Inmueble;
import com.reservo.modelo.reserva.estadosReservas.Cancelado;
import com.reservo.modelo.reserva.estadosReservas.Pendiente;
import com.reservo.modelo.reserva.estadosReservas.Vigente;
import com.reservo.modelo.user.Usuario;
import com.reservo.service.EmailService;
import com.reservo.service.exception.peticion.*;
import com.reservo.modelo.reserva.Peticion;
import com.reservo.modelo.managers.TimeManager;
import com.reservo.persistencia.DAO.PeticionDAO;
import com.reservo.service.PeticionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class PeticionServiceImpl implements PeticionService {

    private final PeticionDAO peticionDAO;
    private final TimeManager timeManager;
    private final EmailService emailService;

    public PeticionServiceImpl(PeticionDAO peticionDAO, TimeManager timeManager, EmailService emailService) {
        this.timeManager = timeManager;
        this.peticionDAO = peticionDAO;
        this.emailService = emailService;
    }

    @Override
    public Peticion create(Peticion peticion) {
        verificarDisponibilidad(peticion);
        verificarSiYaSeRealizoLaPeticion(peticion);
        verificarSiEsDuenioDeLaPropiedad(peticion);
        verificarSiOrdenDeHorarioDeLaPeticion(peticion);
        verificarSeDentroDelRango(peticion);
        verificarSiEsUnViajeroDelTiempo(peticion);
        return peticionDAO.save(peticion);
    }

    private void verificarSiEsDuenioDeLaPropiedad(Peticion peticion) {
        if (esDuenio(peticion)) throw new EsDueñoDeLaPropiedadSolicitada();
    }

    private boolean esDuenio(Peticion peticion) {
        return Objects.equals(peticion.getCliente().getId(), peticion.getInmueble().getOwner().getId());
    }

    private void verificarSiEsUnViajeroDelTiempo(Peticion peticion) {
        if (!timeManager.esActual(peticion)) throw new VieneDelPasado();
    }

    private void verificarSiYaSeRealizoLaPeticion(Peticion peticion) {
        if (existeUnaPeticionHecha(peticion)) throw new RealizoUnaPeticionSobreElInmuebleEnElMismoDia();
    }

    private boolean existeUnaPeticionHecha(Peticion peticion) {
        return peticionDAO.findByUsuarioAndInmueble(peticion.getCliente(), peticion.getInmueble(), peticion.getFechaDelEvento()).isPresent();
    }

    private void verificarSiOrdenDeHorarioDeLaPeticion(Peticion peticion) {
        if (!timeManager.estanOrdenadosLosHorarios(peticion)) throw new HorarioDesordenado();
    }

    private void verificarSeDentroDelRango(Peticion peticion) {
        if (!timeManager.estaDentroDelRango(peticion)) throw new RangoDeHorarioSuperado();
    }


    private void verificarDisponibilidad(Peticion peticion) {
        if(timeManager.elRangoEstaOcupadoPorAlgunaPeticion(peticion, peticionDAO)) throw new HorariosSuperpuestos();
    }

    @Override
    public Optional<Peticion> findById(Long peticionId) {
        return peticionDAO.findById(peticionId);
    }


    @Override
    public List<Peticion> findAll() {return peticionDAO.findAll();}

    @Override
    public void update(Peticion peticion) {peticionDAO.save(peticion);}

    @Override
    public void delete(Peticion peticion) {peticionDAO.delete(peticion);}

    @Override
    public List<Peticion> findAllVigentesByDateInInmueble(Long inmuebleId, LocalDate date) {
        return peticionDAO.findAllVigentesByDateInInmueble(inmuebleId, date);
    }

    @Override
    public void reject(RechazoDTO rechazoDTO) {
        if (peticionDAO.findVigenteById(rechazoDTO.peticionId()).isPresent()) throw new PeticionYaVigente("La petición ya está vigente.");
        if (peticionDAO.itsDeprecatedFromDateAndTime(rechazoDTO.peticionId(), LocalDate.now(), LocalTime.now())) throw new PeticionVencida("La petición esta vencida.");
        if (!peticionDAO.isPetitionOfOwner(rechazoDTO.peticionId(), rechazoDTO.ownerId())) return;

        Peticion peticion = peticionDAO.findPendienteById(rechazoDTO.peticionId()).get(); // no debería tirar error porque el check de arriba hace return si no existe también

        peticion.rechazar(rechazoDTO.motivoDeRechazo());
        peticionDAO.save(peticion);
        sendRejectMessageToClient(peticion);
    }

    @Override
    public void cancel(CancelacionDTO cancelcaionDTO){
        Optional<Peticion> optionalPeticion = peticionDAO.findById(cancelcaionDTO.peticionId());

        if (optionalPeticion.isEmpty()) return;
        if (peticionDAO.itsDeprecatedFromDateAndTime(cancelcaionDTO.peticionId(), LocalDate.now(), LocalTime.now())) throw new PeticionVencida("La petición esta vencida.");
        if (peticionDAO.findRejectedById(cancelcaionDTO.peticionId()).isPresent()) throw new PeticionYaVigente("La petición ya fue cancelada.");

        Peticion peticion = optionalPeticion.get();

        double monto = peticion.cancelar(cancelcaionDTO.motivoDeCancelacion());

        peticionDAO.save(peticion);
        sendCancelMessageToClient(peticion);
        this.sendMessageCancelation(peticion, monto);
    }

    private void sendMessageCancelation(Peticion peticion, double monto) {
        if(peticion.getPagado()){
            sendAppPoliticaCancelationPaidMessageToOwner(peticion, monto);
            sendAppPoliticCancelationPaidMessageToClient(peticion, monto);

        } else {
            sendAppPoliticaCancelationNotPaidMessageToOwner(peticion, monto);
            sendAppPoliticCancelationNotPaidMessageToClient(peticion, monto);

        }
    }

    private void sendAppPoliticaCancelationPaidMessageToOwner(Peticion peticion, double reintegro) {
        String propertyDataText = """
        Le informamos desde Reservo que debido a la cancelación de la petición al inmueble %s
        en el horario de %s - %s, se le aplicó la política de cancelación %s
        al cliente %s (%s), por lo que deberá realizarle un reintegro de $%s.
        """;

        sendMessageToOwnerCancelation(peticion, reintegro, propertyDataText);
    }

    private void sendAppPoliticaCancelationNotPaidMessageToOwner(Peticion peticion, double multa) {
        String propertyDataText = """
        Le informamos desde Reservo que debido a la cancelación de la petición al inmueble %s
        en el horario de %s - %s, se aplicó la política de cancelación %s
        al cliente %s (%s) con un monto a abonar de $%s.
        """;

        sendMessageToOwnerCancelation(peticion, multa, propertyDataText);
    }

    private void sendMessageToOwnerCancelation(Peticion peticion, double monto, String propertyDataText) {
        String ownerEmail = peticion.getInmueble().getOwner().getEmail();
        String subject = "Aplicación de política de cancelación";



        Inmueble inmueble = peticion.getInmueble();
        Usuario cliente = peticion.getCliente();

        String propertyName = EmailMessages.getOrangeHTMLSpan(inmueble.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String horaInicio = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraInicio()));
        String horaFin = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraFin()));
        String politicaNombre = EmailMessages.getOrangeHTMLSpan(peticion.getPoliticaCancelacion().getTipo());
        String nombreCliente = EmailMessages.getOrangeHTMLSpan(cliente.getName());
        String emailCliente = EmailMessages.getOrangeHTMLSpan(cliente.getEmail());
        String montoText = EmailMessages.getOrangeHTMLSpan(String.format("%.2f", monto));

        String htmlContent = EmailMessages.getHTML(
                propertyDataText.formatted(
                        propertyName,
                        horaInicio,
                        horaFin,
                        politicaNombre,
                        nombreCliente,
                        emailCliente,
                        montoText
                ),
                ""
        );

        new Thread(() -> {
            try {
                emailService.sendHTMLEmail(ownerEmail, subject, htmlContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    @Override
    public void approve(Long peticionId) {
        Optional<Peticion> optionalPeticion = peticionDAO.findPendienteById(peticionId);
        if (optionalPeticion.isEmpty()) return;

        Peticion peticion = optionalPeticion.get();
        if (peticionDAO.itsDeprecatedFromDateAndTime(peticionId, LocalDate.now(), LocalTime.now())) throw new PeticionVencida("La petición esta vencida.");
        Long inmuebleId = peticion.getInmueble().getId();
        if (peticionDAO.wasAcceptedInSameTimeRange(inmuebleId, peticion.getFechaDelEvento(), peticion.getHoraInicio(), peticion.getHoraFin())) throw new HorarioOcupado("El horario ya esta ocupado por otra petición.");

        peticion.aprobar();
        peticionDAO.save(peticion);
        sendApproveMessageToClient(peticion);

    }

    private void sendAppPoliticCancelationNotPaidMessageToClient(Peticion peticion, double deuda) {
        String propertyDataText = """
            Le informamos desde Reservo que debido a la cancelación de la petición al inmueble %s
            en el horario de %s - %s, se le aplicó la política de cancelación %s
            con un monto de $%s que deberá abonarle al dueño del inmueble %s (%s).
            """;

        sendMessageToClientCancelationFine(peticion, deuda, propertyDataText);
    }

    private void sendAppPoliticCancelationPaidMessageToClient(Peticion peticion, double reintegro) {
        String propertyDataText = """
            Le informamos desde Reservo que debido a la cancelación de la petición al inmueble %s
            en el horario de %s - %s, se le aplicó la política de cancelación %s,
            por lo que el dueño del inmueble %s (%s) deberá realizarle un reintegro de $%s.
            """;

        sendMessageToClientCancelation(peticion, reintegro, propertyDataText);
    }

    private void sendMessageToClientCancelation(Peticion peticion, double reintegro, String propertyDataText) {
        String clientEmail = peticion.getCliente().getEmail();
        String subject = "Aplicación de política de cancelación";

        Inmueble inmueble = peticion.getInmueble();
        Usuario duenio = inmueble.getOwner();

        String propertyName = EmailMessages.getOrangeHTMLSpan(inmueble.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String horaInicio = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraInicio()));
        String horaFin = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraFin()));
        String politicaNombre = EmailMessages.getOrangeHTMLSpan(peticion.getPoliticaCancelacion().getTipo());
        String nombreDuenio = EmailMessages.getOrangeHTMLSpan(duenio.getName());
        String emailDuenio = EmailMessages.getOrangeHTMLSpan(duenio.getEmail());
        String reintegroText = EmailMessages.getOrangeHTMLSpan(String.format("%.2f", reintegro));

        String htmlContent = EmailMessages.getHTML(
                propertyDataText.formatted(
                        propertyName,
                        horaInicio,
                        horaFin,
                        politicaNombre,
                        nombreDuenio,
                        emailDuenio,
                        reintegroText
                ),
                ""
        );

        new Thread(() -> emailService.sendHTMLEmail(clientEmail, subject, htmlContent)).start();
    }

    private void sendMessageToClientCancelationFine(Peticion peticion, double multa, String propertyDataText) {
        String clientEmail = peticion.getCliente().getEmail();
        String subject = "Aplicación de política de cancelación";

        Inmueble inmueble = peticion.getInmueble();
        Usuario duenio = inmueble.getOwner();

        String propertyName = EmailMessages.getOrangeHTMLSpan(inmueble.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String horaInicio = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraInicio()));
        String horaFin = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraFin()));
        String politicaNombre = EmailMessages.getOrangeHTMLSpan(peticion.getPoliticaCancelacion().getTipo());
        String multaText = EmailMessages.getOrangeHTMLSpan(String.format("%.2f", multa));
        String nombreDuenio = EmailMessages.getOrangeHTMLSpan(duenio.getName());
        String emailDuenio = EmailMessages.getOrangeHTMLSpan(duenio.getEmail());

        String htmlContent = EmailMessages.getHTML(
                propertyDataText.formatted(
                        propertyName,
                        horaInicio,
                        horaFin,
                        politicaNombre,
                        multaText,
                        nombreDuenio,
                        emailDuenio
                ),
                "" // No hace falta texto adicional
        );

        new Thread(() -> emailService.sendHTMLEmail(clientEmail, subject, htmlContent)).start();
    }


    private void sendApproveMessageToClient(Peticion peticion) {
        String propertyDataText = "Le informamos desde Reservo que su petición al inmueble %s, en la localidad de %s, dirección %s %s, ha sido aceptada por el dueño en el horario de";
        String dateText = "%s - %s para el día %s.";
        sendMessageToClient(peticion, propertyDataText, dateText);
    }

    private void sendRejectMessageToClient(Peticion peticion) {
        String rejectionSpan = EmailMessages.getOrangeHTMLSpan("\"" + peticion.getMotivoCancelacionRechazo() + "\"");
        String rejectPartOfMessage = (!peticion.getMotivoCancelacionRechazo().isBlank()) ? " bajo el motivo: %s.".formatted(rejectionSpan) : ".";

        String propertyDataText ="Le informamos desde Reservo que su petición al inmueble %s, en la localidad de %s, dirección %s %s,";
        String dateText = "en el horario de %s - %s para el día %s, ha sido rechazada por el dueño" + rejectPartOfMessage;

        sendMessageToClient(peticion, propertyDataText, dateText);
    }

    private void sendCancelMessageToClient(Peticion peticion) {
        String propertyDataText ="Le informamos desde Reservo que su petición al inmueble %s, en la localidad de %s, dirección %s %s,";
        String dateText = "en el horario de %s - %s para el día %s, ha sido cancelada correctamente.";

        sendMessageToClient(peticion, propertyDataText, dateText);
    }

    private void sendMessageToClient(Peticion peticion, String propertyDataText, String dateText) {
        String clientEmail = peticion.getCliente().getEmail();
        String subject = "Petición a propiedad";

        Inmueble inmueble = peticion.getInmueble();
        String propertyName = EmailMessages.getOrangeHTMLSpan(inmueble.getName());

        String ubication = EmailMessages.getOrangeHTMLSpan(inmueble.getUbication());
        String calle = EmailMessages.getOrangeHTMLSpan(inmueble.getCalle());
        String altura = EmailMessages.getOrangeHTMLSpan(String.valueOf(inmueble.getAltura()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String horaInicio =  EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraInicio()));
        String horaFin = EmailMessages.getOrangeHTMLSpan(formatter.format(peticion.getHoraFin()));

        String fechaDelEvento = EmailMessages.getOrangeHTMLSpan(String.valueOf(peticion.getFechaDelEvento()));

        String htmlContent = EmailMessages.getHTML(
                propertyDataText.formatted(propertyName, ubication, calle, altura),
                dateText.formatted(horaInicio, horaFin, fechaDelEvento));

        new Thread(() -> emailService.sendHTMLEmail(clientEmail, subject, htmlContent)).start();
    }

    @Override
    public void declararPago(Long peticionId){
        Optional<Peticion> optionalPeticion = peticionDAO.findById(peticionId);
        if (optionalPeticion.isEmpty()) {
            return;
        }
        Peticion peticion= optionalPeticion.get();

        peticion.declararPago();
        peticionDAO.save(peticion);

    }


    @Override
    public Page<Peticion> findAllPendientByOwnerId(Long userId, Pageable pageable) {
        return peticionDAO.findAllPendientByOwnerId(userId, pageable);
    }

    @Override
    public Page<Peticion> findAllApproveByOwnerId(Long userId, Pageable pageable) {
        return peticionDAO.findAllApproveByOwnerId(userId, pageable);
    }

    @Override
    public Page<Peticion> findAllRejectByOwnerId(Long userId, Pageable pageable) {
        return peticionDAO.findAllRejectByOwnerId(userId, pageable);
    }

    @Override
    public Page<Peticion> findAllReservasPendientesByUserId(Long userId, Pageable page) {
        return peticionDAO.findAllReservasByEstado(userId, page, Pendiente.class);
    }

    @Override
    public Page<Peticion> findAllReservasVigentesByUserId(Long userId, Pageable page) {
        return peticionDAO.findAllReservasByEstado(userId, page, Vigente.class);
    }

    @Override
    public Page<Peticion> findAllReservasCanceladasByUserId(Long userId, Pageable page) {
        return peticionDAO.findAllReservasByEstado(userId, page, Cancelado.class);
    }
}
