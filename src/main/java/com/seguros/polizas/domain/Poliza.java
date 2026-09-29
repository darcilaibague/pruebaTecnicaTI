package com.seguros.polizas.domain;

import com.seguros.polizas.exception.BusinessException;
import com.seguros.polizas.exception.InvalidPolicyTypeException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Entity
@Table(name = "polizas")
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado;

    @Column(nullable = false, length = 150)
    private String tomador;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Integer mesesVigencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Column(nullable = false, precision = 17, scale = 2)
    private BigDecimal prima;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() {

    }

    private  Poliza(String numero, TipoPoliza tipo, String tomador, LocalDate fechaInicio, int mesesVigencia, BigDecimal canonMensual) {
        if (mesesVigencia <=0) {
            throw new IllegalArgumentException("La vigencia debe ser de al menos un mes");
        }
        if (canonMensual == null || canonMensual.signum() <= 0){
            throw new IllegalArgumentException("El canon mensual debe ser mayor a cero");
        }
        this.numero = numero;
        this.tipo = tipo;
        this.tomador = tomador;
        this.fechaInicio = fechaInicio;
        this.mesesVigencia = mesesVigencia;
        this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
        this.canonMensual = canonMensual.setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
        this.estado = EstadoPoliza.VIGENTE;
    }

    /** En la individual el tomador y el asegurado son el arrendatario; nace con su único riesgo. */
    public static Poliza individual(String numero, String arrendatario, String arrendador,
                                     String direccionInmueble, LocalDate fechaInicio,
                                     int mesesVigencia, BigDecimal canonMensual) {
        Poliza poliza = new Poliza(numero, TipoPoliza.INDIVIDUAL, arrendatario, fechaInicio, mesesVigencia, canonMensual);
        poliza.incluirRiesgo(direccionInmueble, arrendatario, arrendador);
        return poliza;
    }

    /** En la colectiva el tomador es la inmobiliaria o administración. */
    public static Poliza colectiva(String numero, String tomador, LocalDate fechaInicio,
                                   int mesesVigencia, BigDecimal canonMensual) {
        return new Poliza(numero, TipoPoliza.COLECTIVA, tomador, fechaInicio, mesesVigencia, canonMensual);
    }

    public Riesgo agregarRiesgo(String direccionInmueble, String arrendatario, String arrendador) {
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new InvalidPolicyTypeException(
                    "Solo se pueden agregar riesgos a pólizas colectivas. La póliza " + numero + " es " + tipo);
        }
        if (estaCancelada()) {
            throw new BusinessException("POLIZA_CANCELADA",
                    "No se pueden agregar riesgos a la póliza " + numero + " porque está cancelada");
        }
        return incluirRiesgo(direccionInmueble, arrendatario, arrendador);
    }

    /** Único punto por el que entra un riesgo: aquí se protege la regla de la individual. */
    private Riesgo incluirRiesgo(String direccion, String arrendatario, String arrendador) {
        if (tipo == TipoPoliza.INDIVIDUAL && cantidadRiesgosActivos() >= 1) {
            throw new BusinessException("INDIVIDUAL_UN_RIESGO", "Una póliza individual solo puede tener un riesgo");
        }
        Riesgo riesgo = new Riesgo(this, direccion, arrendatario, arrendador);
        riesgos.add(riesgo);
        return riesgo;
    }

    public void renovar(BigDecimal ipc) {
        if (estaCancelada()) {
            throw new BusinessException("POLIZA_CANCELADA",
                    "No se puede renovar la póliza " + numero + " porque está cancelada");
        }
        if (ipc == null) {
            throw new IllegalArgumentException("El IPC es obligatorio para renovar");
        }
        this.canonMensual = canonMensual.multiply(BigDecimal.ONE.add(ipc)).setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
        this.fechaInicio = fechaFin;
        this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
        this.estado = EstadoPoliza.RENOVADA;
    }

    public void cancelar() {
        if (estaCancelada()) {
            throw new BusinessException("POLIZA_YA_CANCELADA", "La póliza " + numero + " ya está cancelada");
        }
        this.estado = EstadoPoliza.CANCELADA;
        riesgos.stream().filter(Riesgo::estaActivo).forEach(Riesgo::marcarCancelado);
    }

    public boolean estaCancelada() {
        return estado == EstadoPoliza.CANCELADA;
    }

    public long cantidadRiesgosActivos() {
        return riesgos.stream().filter(Riesgo::estaActivo).count();
    }

    private BigDecimal calcularPrima() {
        return canonMensual.multiply(BigDecimal.valueOf(mesesVigencia)).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public TipoPoliza getTipo() {
        return tipo;
    }

    public EstadoPoliza getEstado() {
        return estado;
    }

    public String getTomador() {
        return tomador;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public Integer getMesesVigencia() {
        return mesesVigencia;
    }

    public BigDecimal getCanonMensual() {
        return canonMensual;
    }

    public BigDecimal getPrima() {
        return prima;
    }

    public List<Riesgo> getRiesgos() {
        return Collections.unmodifiableList(riesgos);
    }
}
