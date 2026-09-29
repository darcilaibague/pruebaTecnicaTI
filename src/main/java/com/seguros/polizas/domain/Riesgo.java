package com.seguros.polizas.domain;

import com.seguros.polizas.exception.BusinessException;
import jakarta.persistence.*;

import java.time.LocalDate;


@Entity
@Table(name = "riesgps")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(nullable = false, length = 200)
    private String direccionInmueble;

    /** Asegurado. */
    @Column(nullable = false, length = 150)
    private String arrendatario;

    /** Beneficiario en caso de siniestro. */
    @Column(nullable = false, length = 150)
    private String arrendador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado;

    @Column(nullable = false)
    private LocalDate fechaInclusion;

    private LocalDate fechaCancelacion;

    protected Riesgo() {
        // requerido por JPA
    }

    /** Solo la póliza crea riesgos (ver Poliza#incluirRiesgo). */
    Riesgo(Poliza poliza, String direccionInmueble, String arrendatario, String arrendador) {
        this.poliza = poliza;
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
        this.arrendador = arrendador;
        this.estado = EstadoRiesgo.ACTIVO;
        this.fechaInclusion = LocalDate.now();
    }

    public void cancelar() {
        if (!estaActivo()) {
            throw new BusinessException("RIESGO_YA_CANCELADO", "El riesgo " + id + " ya está cancelado");
        }
        if (poliza.getTipo() == TipoPoliza.INDIVIDUAL) {
            throw new BusinessException("RIESGO_UNICO_INDIVIDUAL",
                    "El riesgo es el único de la póliza individual " + poliza.getNumero()
                            + "; para retirarlo se debe cancelar la póliza");
        }
        marcarCancelado();
    }

    void marcarCancelado() {
        this.estado = EstadoRiesgo.CANCELADO;
        this.fechaCancelacion = LocalDate.now();
    }

    public boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    public Long getId() {
        return id;
    }

    public Poliza getPoliza() {
        return poliza;
    }

    public String getDireccionInmueble() {
        return direccionInmueble;
    }

    public String getArrendatario() {
        return arrendatario;
    }

    public String getArrendador() {
        return arrendador;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }

    public LocalDate getFechaInclusion() {
        return fechaInclusion;
    }

    public LocalDate getFechaCancelacion() {
        return fechaCancelacion;
    }
}
