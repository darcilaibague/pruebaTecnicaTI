package com.seguros.polizas.dto;

import java.time.Instant;

public record ErrorResponse(String codigo, String mensaje, Instant timestamp) {
    public static ErrorResponse of(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, Instant.now());
    }
}
