package com.seguros.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiesgoRequest(@NotBlank(message = "es obligatoria") @Size(max = 200) String direccionInmueble,
                            @NotBlank(message = "es obligatorio") @Size(max = 150) String arrendatario,
                            @NotBlank(message = "es obligatorio") @Size(max = 150) String arrendador) {
}
