package cl.duoc.cloudnative.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuiaRequest(
        @NotBlank String transportista,
        @NotBlank String credencialDescarga,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "fecha debe usar formato YYYYMM") String fecha,
        @NotBlank String contenido
) {
}
