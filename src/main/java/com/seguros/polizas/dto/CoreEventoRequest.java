package com.seguros.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoreEventoRequest(
    @NotBlank(message = "es obligatorio") String evento,
    @NotNull(message = "es obligatorio") Long polizaId) {
}
