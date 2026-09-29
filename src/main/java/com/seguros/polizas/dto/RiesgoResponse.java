package com.seguros.polizas.dto;

import com.seguros.polizas.domain.EstadoRiesgo;
import com.seguros.polizas.domain.Riesgo;

import java.time.LocalDate;

public record RiesgoResponse(Long id,
                             Long polizaId,
                             String direccionInmueble,
                             String arrendatario,
                             String arrendador,
                             EstadoRiesgo estado,
                             LocalDate fechaInclusion,
                             LocalDate fechaCancelacion) {

    public static RiesgoResponse from(Riesgo riesgo) {
        return new RiesgoResponse(
                riesgo.getId(),
                riesgo.getPoliza().getId(),
                riesgo.getDireccionInmueble(),
                riesgo.getArrendatario(),
                riesgo.getArrendador(),
                riesgo.getEstado(),
                riesgo.getFechaInclusion(),
                riesgo.getFechaCancelacion());
    }
}
