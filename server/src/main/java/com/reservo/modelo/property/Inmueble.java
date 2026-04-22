package com.reservo.modelo.property;

import com.reservo.modelo.property.enums.DiasDeLaSemana;
import com.reservo.modelo.politicasDeCancelacion.PoliticaDeCancelacion;
import com.reservo.modelo.user.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(force = true)
@ToString

@Entity
public class Inmueble {
    public static final String LOCALHOST_8081_UPLOADS = "http://localhost:8081/uploads/";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private  String name;

    @Column(nullable = false)
    private  String description;

    @Column(nullable = false)
    private  Double price;

    @Column(nullable = false)
    private  String ubication;

    @Column(nullable = false)
    private  Integer capacity;

    @Column(nullable = false)
    private  String conditions;

    @ManyToOne(fetch = FetchType.EAGER)
    private Usuario owner;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private String calle;

    @Column(nullable = false)
    private Integer altura;

    @Column(nullable = false)
    private List<DiasDeLaSemana> availableDays;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "politicaId")
    private PoliticaDeCancelacion cancellation;

    @OneToMany(fetch = FetchType.EAGER)
    private List<ReservoImage> images = new ArrayList<>();

    public Inmueble(String name, String desc, Double price,
                    String ubi, Integer capacity, String condition,
                    LocalTime horaInicio, LocalTime horaFinal, List<DiasDeLaSemana> availableDays,
                    PoliticaDeCancelacion cancellation,List<ReservoImage> images,Usuario owner,String calle,Integer altura) {
        this.name = name;
        this.description = desc;
        this.price = price;
        this.ubication = ubi;
        this.capacity = capacity;
        this.conditions = condition;
        this.owner = owner;
        this.horaInicio = horaInicio;
        this.horaFin = horaFinal;
        this.availableDays = availableDays;
        this.cancellation = cancellation;
        this.images.addAll(images);
        this.calle = calle;
        this.altura = altura;
    }

    public Inmueble(String name, String desc, Double price,
                    String ubi, Integer capacity, String condition,
                    LocalTime horaInicio, LocalTime horaFinal, Usuario owner,
                    PoliticaDeCancelacion cancellation,String calle,Integer altura) {
        this.name = name;
        this.description = desc;
        this.price = price;
        this.ubication = ubi;
        this.capacity = capacity;
        this.conditions = condition;
        this.horaInicio = horaInicio;
        this.horaFin = horaFinal;
        this.owner = owner;
        this.cancellation = cancellation;
        this.calle = calle;
        this.altura = altura;
    }

    public String getFirstImageURL() {
        if (images != null && !images.isEmpty()) {
//            return LOCALHOST_8081_UPLOADS + images.get(0);
                return images.getFirst().getUrl();
        }
        return null;
    }
}
