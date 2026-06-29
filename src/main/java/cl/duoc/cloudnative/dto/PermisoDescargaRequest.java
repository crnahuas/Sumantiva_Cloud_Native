package cl.duoc.cloudnative.dto;

import jakarta.validation.constraints.NotBlank;

public record PermisoDescargaRequest(@NotBlank String credencialDescarga) {
}
