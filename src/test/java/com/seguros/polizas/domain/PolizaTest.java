package com.seguros.polizas.domain;


import com.seguros.polizas.exception.BusinessException;
import com.seguros.polizas.exception.InvalidPolicyTypeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static  org.assertj.core.api.Assertions.assertThat;
import static  org.assertj.core.api.Assertions.assertThatThrownBy;
class PolizaTest {
   private static final BigDecimal IPC = new BigDecimal("0.052");

    private Poliza colectivaConDosRiesgos() {
        Poliza poliza = Poliza.colectiva("POL-T-001", "Inmobiliaria Test",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
        poliza.agregarRiesgo("Dirección 1", "Arrendatario 1", "Arrendador 1");
        poliza.agregarRiesgo("Dirección 2", "Arrendatario 2", "Arrendador 2");
        return poliza;
    }

    private Poliza individual() {
        return Poliza.individual("POL-T-002", "Arrendatario", "Arrendador", "Dirección",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
    }

    @Test
    void laPrimaEsElCanonPorLosMesesDeVigencia() {
        assertThat(colectivaConDosRiesgos().getPrima()).isEqualByComparingTo("12000000");
    }

    @Test
    void renovarIncrementaCanonYPrimaSegunIpc() {
        Poliza poliza = colectivaConDosRiesgos();

        poliza.renovar(IPC);

        assertThat(poliza.getCanonMensual()).isEqualByComparingTo("1052000");
        assertThat(poliza.getPrima()).isEqualByComparingTo("12624000");
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(poliza.getFechaInicio()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(poliza.getFechaFin()).isEqualTo(LocalDate.of(2028, 1, 1));
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() {
        Poliza poliza = colectivaConDosRiesgos();
        poliza.cancelar();

        assertThatThrownBy(() -> poliza.renovar(IPC))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void cancelarLaPolizaCancelaTodosSusRiesgos() {
        Poliza poliza = colectivaConDosRiesgos();

        poliza.cancelar();

        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(poliza.getRiesgos()).allMatch(r -> r.getEstado() == EstadoRiesgo.CANCELADO);
        assertThat(poliza.cantidadRiesgosActivos()).isZero();
    }

    @Test
    void noSePuedeCancelarDosVecesLaMismaPoliza() {
        Poliza poliza = colectivaConDosRiesgos();
        poliza.cancelar();

        assertThatThrownBy(poliza::cancelar).isInstanceOf(BusinessException.class);
    }

    @Test
    void unaPolizaIndividualNaceConExactamenteUnRiesgo() {
        assertThat(individual().getRiesgos()).hasSize(1);
    }

    @Test
    void noSePuedenAgregarRiesgosAUnaPolizaIndividual() {
        Poliza poliza = individual();

        assertThatThrownBy(() -> poliza.agregarRiesgo("Otra dirección", "Otro", "Otro"))
                .isInstanceOf(InvalidPolicyTypeException.class);
        assertThat(poliza.getRiesgos()).hasSize(1);
    }

    @Test
    void noSePuedenAgregarRiesgosAUnaColectivaCancelada() {
        Poliza poliza = colectivaConDosRiesgos();
        poliza.cancelar();

        assertThatThrownBy(() -> poliza.agregarRiesgo("Dirección 3", "A", "B"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void cancelarUnRiesgoDeColectivaNoAfectaALosDemas() {
        Poliza poliza = colectivaConDosRiesgos();

        poliza.getRiesgos().get(0).cancelar();

        assertThat(poliza.cantidadRiesgosActivos()).isEqualTo(1);
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.VIGENTE);
    }

    @Test
    void noSePuedeCancelarElUnicoRiesgoDeUnaIndividual() {
        Riesgo riesgo = individual().getRiesgos().get(0);

        assertThatThrownBy(riesgo::cancelar)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelar la póliza");
    }

}
