package com.reservo.service.impl;

import com.reservo.controller.dto.Inmueble.InmuebleModifyRequestDTO;
import com.reservo.controller.dto.Inmueble.InmuebleRemoveImagesDTO;
import com.reservo.controller.dto.Peticion.RechazoDTO;
import com.reservo.controller.exception.ParametroIncorrecto;
import com.reservo.modelo.Filtro;
import com.reservo.modelo.politicasDeCancelacion.Flexible;
import com.reservo.modelo.property.*;
import com.reservo.modelo.property.enums.DiasDeLaSemana;
import com.reservo.modelo.politicasDeCancelacion.SinDevolucion;
import com.reservo.modelo.reserva.Peticion;
import com.reservo.modelo.user.Usuario;
import com.reservo.service.InmuebleService;
import com.reservo.service.PeticionService;
import com.reservo.service.ResetService;
import com.reservo.service.UsuarioService;
import com.reservo.service.exception.EmailRepetido;
import com.reservo.service.exception.InmuebleRepetidoException;
import com.reservo.service.exception.NoExisteInmuebleExpcetion;
import com.reservo.service.exception.TienePeticionVigenteException;
import com.reservo.testUtils.TestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
public class InmuebleServiceImplTest {


    @Autowired
    private TestService testService;

    @Autowired
    private InmuebleService inmuebleService;

    @Autowired
    private UsuarioService userService;

    @Autowired
    private PeticionService peticionService;

    @Autowired
    private ResetService resetService;


    private Usuario jorge;
    private Usuario juan;
    private Inmueble inmueble1;
    private Inmueble inmueble2;
    private List<MultipartFile> emptyImages;
    private InmuebleModifyRequestDTO inmuebleDTO1;
    private MockMultipartFile mockImage;
    private InmuebleRemoveImagesDTO removeImagesDTO;
    Peticion peticionDeJuanito;

    @AfterEach
    void tearDown() {
        resetService.resetAll();
    }

    @BeforeEach
    public void setUp() throws EmailRepetido {

        jorge = new Usuario("jorge", "aa21", "jorge@yahoo.com.ar");
        juan = new Usuario("juan", "aa22", "juan@yahoo.com.ar");

        inmueble1 = new Inmueble(
                "Plaza", "Es una plaza linda", 200d,"Berazategui", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(),"lavalle",987);

        inmueble2 = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d,"Quilmes", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), juan, new SinDevolucion(),"pelegrini",123);

        emptyImages = Collections.emptyList();
        inmueble1.setAvailableDays(Collections.emptyList());
        inmueble2.setAvailableDays(Collections.emptyList());

        List<DiasDeLaSemana> diasDTOInmueble = List.of(DiasDeLaSemana.LUNES);
        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        mockImage = new MockMultipartFile(
                "image",
                "pepeEnLaDucha.jpg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        removeImagesDTO = new InmuebleRemoveImagesDTO(List.of(0));

        userService.create(jorge);
        userService.create(juan);


        peticionDeJuanito = new Peticion(juan, inmueble1, LocalDate.now().plusDays(10), LocalTime.of(13, 0), LocalTime.of(14, 0), 100D);

    }

    @Test
    public void alRecuperarUnIdInexistenteNoTraeNada() {
        Optional<Inmueble> in = inmuebleService.findById(Long.valueOf(-3L));

        assertFalse(in.isPresent());
    }

    @Test
    void seGuardaUnInmuebleEnlaDB() throws EmailRepetido {
        userService.create(jorge);


        inmuebleService.create(inmueble1,emptyImages);
        Optional<Inmueble> in = inmuebleService.findById(inmueble1.getId());

        assertTrue(in.isPresent());
    }

    @Test
    void sePersistenLosDatosDelInmueble() throws EmailRepetido {
        userService.create(jorge);
        inmuebleService.create(inmueble1,emptyImages);
        Optional<Inmueble> in = inmuebleService.findById(inmueble1.getId());

        assertEquals("Plaza", in.get().getName());
        assertEquals("Es una plaza linda", in.get().getDescription());
        assertEquals(200d, in.get().getPrice());
        assertEquals("Berazategui", in.get().getUbication());
        assertEquals(100, in.get().getCapacity());
        assertEquals("No romper nada", in.get().getConditions());
        assertEquals(jorge.getId(), in.get().getOwner().getId());
    }

    @Test
    void sePersistenVariasPropiedades() throws EmailRepetido {

        inmuebleService.create(inmueble1,emptyImages);
        inmuebleService.create(inmueble2,emptyImages);

        List<Inmueble> inmuebles = inmuebleService.findAll();

        assertEquals(2, inmuebles.size());
    }
    @Test
    public void seBuscaLosInmueblesConLaInicialPYTraeDosPaginas() throws EmailRepetido {
        userService.create(jorge);


        for (int i = 0; i < 10; i++) {
            Inmueble inm = new Inmueble(
                    "Plaza" + i, "Es un lugar espacioso", 200d, "Quilmes", 100, "No romper nada",
                    LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
            inm.setAvailableDays(Collections.emptyList());
            inmuebleService.create(inm, emptyImages);
        }


        int pageSize = 5;


        Filtro filtroPagina1 = new Filtro(
                "",
                "P",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertFalse(pagina1.isEmpty());
        assertEquals(pageSize, pagina1.getContent().size());
        pagina1.getContent().forEach(inmueble -> {
            assertTrue(inmueble.getName().startsWith("P"));
        });


        Filtro filtroPagina2 = new Filtro(
                "",
                "P",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(1, pageSize)
        );
        Page<Inmueble> pagina2 = inmuebleService.findByFiltro(filtroPagina2);

        assertFalse(pagina2.isEmpty());
        assertEquals(pageSize, pagina2.getContent().size());
        pagina2.getContent().forEach(inmueble -> {
            assertTrue(inmueble.getName().startsWith("P"));
        });


        assertNotEquals(pagina1.getContent(), pagina2.getContent());

        assertEquals(2, pagina1.getTotalPages());
        assertEquals(2, pagina2.getTotalPages());
    }

    @Test
    void noPuedenHaberDosUsuariosConElInmueble() throws EmailRepetido {

        inmuebleService.create(inmueble1,emptyImages);

        assertThrows(InmuebleRepetidoException.class, () -> {inmuebleService.create(inmueble1,emptyImages);});
    }

    @Test
    void seActualizaUnInmuebleYCambiaSuNombreAlNuevo() throws EmailRepetido, ParametroIncorrecto {

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("Palacio de la bondad", inmuebleFromDb.getName());
    }

    @Test
    void seActualizaUnInmuebleYCambiaSuDescripcionALaNueva() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("full bondad pa", inmuebleFromDb.getDescription());
    }

    @Test
    void seActualizaUnInmuebleYCambiaSuLocalidadALaNueva() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("Quilmes", inmuebleFromDb.getUbication());
    }

    @Test
    void seActualizaUnInmuebleYCambiaSuPrecioAlNuevo() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(18000, inmuebleFromDb.getPrice());
    }


    @Test
    void seActualizaUnInmuebleYCambiaSuCapacidadALaNueva() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(35, inmuebleFromDb.getCapacity());
    }

    @Test
    void seActualizaUnInmuebleYCambiaSusCondicionesALasNuevas() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("romper todo", inmuebleFromDb.getConditions());
    }

    @Test
    void seActualizaUnInmuebleParcialmenteYCambiaSusCondicionesALasNuevas() throws EmailRepetido, ParametroIncorrecto {
        inmuebleDTO1 = new InmuebleModifyRequestDTO(null, "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", null, null, "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("romper todo", inmuebleFromDb.getConditions());
    }

    @Test
    void seActualizaUnInmuebleYCambiaSusHorariosALosNuevos() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(LocalTime.parse("10:00"), inmuebleFromDb.getHoraInicio());
        assertEquals(LocalTime.parse("18:00"), inmuebleFromDb.getHoraFin());
    }

    @Test
    void seActualizaUnInmuebleYCambiaSusDiasDisponiblesALosNuevos() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(List.of(DiasDeLaSemana.LUNES), inmuebleFromDb.getAvailableDays());

    }

    @Test
    void seActualizaUnInmuebleYCambiaSuPoliticaDeCancelacionALaNueva() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(Flexible.class, inmuebleFromDb.getCancellation().getClass());

    }

    @Test
    void seActualizaUnInmuebleYCambiaSuDireccionALaNueva() throws EmailRepetido, ParametroIncorrecto {
//        inmuebleDTO1 = new InmuebleModifyRequestDTO("Palacio de la bondad", "full bondad pa", "Quilmes", 18000d, 35, "romper todo", "10:00", "18:00", diasDTOInmueble, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("Balcarce", inmuebleFromDb.getCalle());
        assertEquals(50, inmuebleFromDb.getAltura());

    }

    @Test
    void seActualizaUnInmuebleParcialmenteYCambiaSuDireccionALaNueva() throws EmailRepetido, ParametroIncorrecto {
        inmuebleDTO1 = new InmuebleModifyRequestDTO(null, "full bondad pa", null, 18000d, 35, "romper todo", "10:00", "18:00", null, "Flexible", "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("Balcarce", inmuebleFromDb.getCalle());
        assertEquals(50, inmuebleFromDb.getAltura());

    }

    @Test
    void seActualizaUnInmuebleParcialmenteYCambianLasCosasQueSeEspecifican() throws EmailRepetido, ParametroIncorrecto {
        inmuebleDTO1 = new InmuebleModifyRequestDTO(null, null, null, 18000d, 35, null, "10:00", "18:00", null, null, "Balcarce", 50);

        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.update(inmueble1.getId(), inmuebleDTO1);

        Inmueble inmuebleFromDb = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals("Balcarce", inmuebleFromDb.getCalle());
        assertEquals(50, inmuebleFromDb.getAltura());
        assertEquals(LocalTime.parse("10:00"), inmuebleFromDb.getHoraInicio());
        assertEquals(LocalTime.parse("18:00"), inmuebleFromDb.getHoraFin());
        assertEquals(18000, inmuebleFromDb.getPrice());
        assertEquals(35, inmuebleFromDb.getCapacity());

    }
    @Disabled
    @Test
    void noSeActualizaLaImagenDeUnInmuebleCuandoSeMandaNada() throws ParametroIncorrecto {
        inmuebleDTO1 = new InmuebleModifyRequestDTO(null, null, null, null, null, null, null, null, null, null, null, null);


        inmueble1.setImages(List.of(new ReservoImage("pepe", "c://pepe.png")) );
        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.addImages(inmueble1.getId(), emptyImages);

        Inmueble inmuebleFromDb2 = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(1,  inmuebleFromDb2.getImages().size());

    }
    @Disabled
    @Test
    void seAgregaUnaImagenDeUnInmueble() throws ParametroIncorrecto {


        inmueble1.setImages(List.of(new ReservoImage("pepe", "c://pepe.png")) );
        inmuebleService.create(inmueble1,emptyImages);

        inmuebleService.addImages(inmueble1.getId(), List.of(mockImage));

        Inmueble inmuebleFromDb2 = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(2,  inmuebleFromDb2.getImages().size());

    }
    @Disabled
    @Test
    void seQuitaUnaImagenDeUnInmueble() throws ParametroIncorrecto {


        inmuebleService.create(inmueble1,List.of(mockImage)); // 1

        inmuebleService.removeImages(inmueble1.getId(), removeImagesDTO);

        Inmueble inmuebleFromDb2 = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(0,  inmuebleFromDb2.getImages().size());

    }
    @Disabled
    @Test
    void seQuitanTodasLasImagenesDeUnInmueble() throws EmailRepetido, ParametroIncorrecto {
        removeImagesDTO = new InmuebleRemoveImagesDTO(List.of(0, 1));


        inmuebleService.create(inmueble1,List.of(mockImage, mockImage)); // 2

        inmuebleService.removeImages(inmueble1.getId(), removeImagesDTO);

        Inmueble inmuebleFromDb2 = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(0,  inmuebleFromDb2.getImages().size());

    }
    @Disabled
    @Test
    void seQuitanImagenesDeUnInmuebleSalteadas() throws EmailRepetido, ParametroIncorrecto {
        removeImagesDTO = new InmuebleRemoveImagesDTO(List.of(0, 1, 3));


        inmuebleService.create(inmueble1,List.of(mockImage, mockImage, mockImage, mockImage)); // 4

        inmuebleService.removeImages(inmueble1.getId(), removeImagesDTO);

        Inmueble inmuebleFromDb2 = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(1,  inmuebleFromDb2.getImages().size());

    }
    @Disabled
    @Test
    void seQuitanImagenesDeUnInmuebleSalteadasDePrincipioAFin() throws EmailRepetido, ParametroIncorrecto {
        removeImagesDTO = new InmuebleRemoveImagesDTO(List.of(0, 3));


        inmuebleService.create(inmueble1,List.of(mockImage, mockImage, mockImage, mockImage)); // 4

        inmuebleService.removeImages(inmueble1.getId(), removeImagesDTO);

        Inmueble inmuebleFromDb2 = inmuebleService.findById(inmueble1.getId()).get();

        assertEquals(2,  inmuebleFromDb2.getImages().size());

    }

    @Test
    void seEliminaUnInmueble() {
        inmuebleService.create(inmueble1, emptyImages);
        Optional<Inmueble> in = inmuebleService.findById(inmueble1.getId());

        assertTrue(in.isPresent());

        inmuebleService.delete(inmueble1.getId());

        in = inmuebleService.findById(inmueble1.getId());

        assertFalse(in.isPresent());
    }

    @Test
    void seIntentaEliminarUnInmuebleQueNoExiste() {
        inmueble1.setId(-1L);

        assertThrows(NoExisteInmuebleExpcetion.class, () -> inmuebleService.delete(inmueble1.getId()));
    }

    @Test
    void seIntentaEliminarUnInmuebleQueTienePeticionesVigentes() {
        inmuebleService.create(inmueble1, emptyImages);

        peticionService.create(peticionDeJuanito);
        peticionService.approve(peticionDeJuanito.getId());

        assertThrows(TienePeticionVigenteException.class, () -> inmuebleService.delete(inmueble1.getId()));
    }

    @Test
    void seEliminaUnInmuebleYSusPeticionesPendientes() {
        inmuebleService.create(inmueble1, emptyImages);
        Optional<Inmueble> in = inmuebleService.findById(inmueble1.getId());
        assertTrue(in.isPresent());

        peticionService.create(peticionDeJuanito);
        Optional<Peticion> pt = peticionService.findById(peticionDeJuanito.getId());
        assertTrue(pt.isPresent());

        inmuebleService.delete(inmueble1.getId());

        in = inmuebleService.findById(inmueble1.getId());
        assertFalse(in.isPresent());
        pt = peticionService.findById(peticionDeJuanito.getId());
        assertFalse(pt.isPresent());
    }

    @Test
    void seEliminaUnInmuebleYSusPeticionesCanceladas() {
        inmuebleService.create(inmueble1, emptyImages);
        Optional<Inmueble> in = inmuebleService.findById(inmueble1.getId());
        assertTrue(in.isPresent());

        peticionService.create(peticionDeJuanito);
        peticionService.reject(new RechazoDTO(peticionDeJuanito.getInmueble().getOwner().getId(), peticionDeJuanito.getId(), "Por gil"));

        inmuebleService.delete(inmueble1.getId());

        in = inmuebleService.findById(inmueble1.getId());
        assertFalse(in.isPresent());
        Optional<Peticion> pt = peticionService.findById(peticionDeJuanito.getId());
        assertFalse(pt.isPresent());
    }
    @Test
    public void seBuscaUbicacionDeInmuebleConTildesYTraeResultadosComoSiNoTuviera() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                    "Quincho", "Es un lugar espacioso", 200d, "Moron", 100, "No romper nada",
                    LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "morón",
                "q",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("Quincho", inmueble.getName());
        assertEquals("Moron", inmueble.getUbication());

    }

    @Test
    public void seBuscaUbicacionDeInmuebleSinTildesYTraeResultadosComoSiTuviera() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "moron",
                "q",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("Quincho", inmueble.getName());
        assertEquals("Morón", inmueble.getUbication());

    }

    @Test
    public void seBuscaNombreDeInmuebleConTildesYTraeResultadosComoSiNoTuviera() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "ia", "Es un lugar espacioso", 200d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "í",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("ia", inmueble.getName());
        assertEquals("Morón", inmueble.getUbication());

    }

    @Test
    public void seBuscaNombreDeInmuebleSinTildesYTraeResultadosComoSiTuviera() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "íííA", "Es un lugar espacioso", 200d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "i",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("íííA", inmueble.getName());
        assertEquals("Morón", inmueble.getUbication());

    }

    @Test
    public void seBuscaInmuebleConUbicacionParcialYNoTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "m",
                "q",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleConUbicacionExactamenteIgualYTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "Morón",
                "q",
                null,
                null,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertFalse(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleConPrecioDentroDelRangoYTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                100,
                500,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertFalse(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleConPrecioJustoDentroDelRangoMinYTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 100d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                100,
                500,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertFalse(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleConPrecioJustoDentroDelRangoMaxYTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                100,
                500,
                LocalTime.of(0, 0),
                LocalTime.of(23, 59),
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertFalse(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleConPrecioFueraDelRangoMinYNoTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                100,
                200,
                null,
                null,
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleConPrecioFueraDelRangoMaxYNoTraeResultados() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                700,
                900,
                null,
                null,
                null,
                PageRequest.of(0, pageSize)
        );
        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmueblesEnUnHorarioEspecificoYExacto() throws EmailRepetido {
        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(9, 0), LocalTime.of(15, 0), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(15, 0),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertEquals(1, pagina1.getContent().size());
    }

    @Test
    public void seBuscaInmuebleEnUnHorarioPeroNoCoincidePorElMaximo() throws EmailRepetido {
        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(9, 0), LocalTime.of(15, 0), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleEnUnHorarioPeroNoCoincidePorElMinimo() throws EmailRepetido {
        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(9, 0), LocalTime.of(15, 0), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                null,
                null,
                LocalTime.of(10, 0),
                LocalTime.of(15, 0),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmuebleEnUnHorarioQueNadaQueVerConLosDisponibles() throws EmailRepetido {
        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(9, 0), LocalTime.of(15, 0), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                null,
                null,
                LocalTime.of(16, 0),
                LocalTime.of(22, 0),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaInmueblePeroTieneOcupadoElHorarioOcupadoUnDia() throws EmailRepetido {
        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(9, 0), LocalTime.of(15, 0), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        peticionService.create(new Peticion(juan, inm, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), 1000D));

        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(15, 0),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertEquals(1, pagina1.getContent().size());
    }
    @Disabled
    @Test
    public void seBuscaInmueblePeroTieneOcupadaTodaLaSemana() throws EmailRepetido {
        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 500d, "Morón", 100, "No romper nada",
                LocalTime.of(15, 0), LocalTime.of(23, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        for (int i=0; i < 8; i++) {
            Peticion p = peticionService.create(new Peticion(juan, inm,
                    LocalDate.now().plusDays(i),
                    LocalTime.of(22, 0), LocalTime.of(23, 0),
                    1000D));

            peticionService.approve(p.getId());
        }

        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                "",
                "",
                null,
                null,
                LocalTime.of(15, 0),
                LocalTime.of(22, 0),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> pagina1 = inmuebleService.findByFiltro(filtroPagina1);

        assertTrue(pagina1.getContent().isEmpty());

    }

    @Test
    public void seBuscaPorCapacidadYSeTraenResultadosIgualesAEsaCapacidad() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Moron", 100, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                null,
                null,
                null,
                null,
                null,
                null,
                100,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("Quincho", inmueble.getName());
        assertEquals("Moron", inmueble.getUbication());

    }

    @Test
    public void seBuscaPorCapacidadYSeTraenResultadosMayoresAEsaCapacidad() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Moron", 150, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                null,
                null,
                null,
                null,
                null,
                null,
                100,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("Quincho", inmueble.getName());
        assertEquals("Moron", inmueble.getUbication());

    }

    @Test
    public void seBuscaPorCapacidadYNoHayResultadosEnElLimiteDeEsaCapacidad() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Moron", 150, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                null,
                null,
                null,
                null,
                null,
                null,
                149,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("Quincho", inmueble.getName());
        assertEquals("Moron", inmueble.getUbication());

    }

    @Test
    public void seBuscaPorCapacidadYNoHayResultadosPorqueElQueHayEsMayorALaIngresada() throws EmailRepetido {
        userService.create(jorge);


        Inmueble inm = new Inmueble(
                "Quincho", "Es un lugar espacioso", 200d, "Moron", 150, "No romper nada",
                LocalTime.of(12, 30), LocalTime.of(14, 30), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);


        int pageSize = 1;

        Filtro filtroPagina1 = new Filtro(
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                PageRequest.of(0, pageSize)
        );
        Inmueble inmueble = inmuebleService.findByFiltro(filtroPagina1).toList().getFirst();

        assertEquals("Quincho", inmueble.getName());
        assertEquals("Moron", inmueble.getUbication());

    }

    @Test
    public void seBuscaPorNombreYlocalidadEnConjunto() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                "Quilmes",
                "Quincho",
                null, null, null, null, null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(1, resultado.getContent().size());
        assertEquals("Quincho", resultado.getContent().getFirst().getName());
    }


    @Test
    public void seBuscaPorNombreYPrecioEnConjunto() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                null,
                "Quincho",
                150,
                250,
                null, null, null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(1, resultado.getContent().size());
        assertEquals("Quincho", resultado.getContent().getFirst().getName());
    }


    @Test
    public void seBuscaPorNombreYHorarioEnConjunto() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                null,
                "Plaza",
                null, null,
                LocalTime.of(12, 30),
                LocalTime.of(14, 30),
                null,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(1, resultado.getContent().size());
        assertEquals("Plaza", resultado.getContent().getFirst().getName());
    }


    @Test
    public void seBuscaPorNombreYCapacidadEnConjunto() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                null,
                "Quincho",
                null, null,
                null, null,
                100, // capacidad
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(1, resultado.getContent().size());
        assertEquals("Quincho", resultado.getContent().getFirst().getName());
    }


    @Test
    public void seBuscaSinNombre() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                "Quilmes",
                "",
                100,
                300,
                LocalTime.of(12, 30),
                LocalTime.of(14, 30),
                100,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(1, resultado.getContent().size());
        assertEquals("Quincho", resultado.getContent().getFirst().getName());
    }


    @Test
    public void seBuscaPorTodosLosFiltrosYTraeTodosLosResultados() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);

        userService.create(jorge);

        Inmueble inm = new Inmueble(
                "Plaza miseria", "Es un lugar espacioso", 150d, "Berazategui", 150, "No romper nada",
                LocalTime.of(12, 0), LocalTime.of(15, 0), jorge, new SinDevolucion(), "lavalle", 987);
        inm.setAvailableDays(Collections.emptyList());
        inmuebleService.create(inm, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                "Berazategui",
                "plaz",
                100,
                300,
                LocalTime.of(12, 0),
                LocalTime.of(15, 0),
                100,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(2, resultado.getContent().size());
    }


    @Test
    public void seBuscaPorTodosLosFiltrosYTraeAlgunosLosResultados() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                "Quilmes",
                "Qui",
                100,
                300,
                LocalTime.of(12, 30),
                LocalTime.of(14, 30),
                100,
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertEquals(1, resultado.getContent().size());
        assertEquals("Quincho", resultado.getContent().getFirst().getName());
    }


    @Test
    public void seBuscaPorTodosLosFiltrosYNoTraeResultados() throws EmailRepetido {
        inmuebleService.create(inmueble1, emptyImages);
        inmuebleService.create(inmueble2, emptyImages);

        int pageSize = 5;

        Filtro filtro = new Filtro(
                "Avellaneda", // No existe Avellaneda entre los inm
                "Casa", // No existe "Casa" entre los inm
                5000,
                8000,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                1000, // se va una locura
                PageRequest.of(0, pageSize)
        );

        Page<Inmueble> resultado = inmuebleService.findByFiltro(filtro);

        assertTrue(resultado.isEmpty());
    }

    @AfterEach
    void limpiarDb(){
        testService.eliminarPeticiones();
        testService.eliminarInmuebles();
        testService.eliminarUsuarios();
    }

}
