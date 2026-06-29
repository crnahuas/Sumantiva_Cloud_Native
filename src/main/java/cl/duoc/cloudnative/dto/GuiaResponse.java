package cl.duoc.cloudnative.dto;

import cl.duoc.cloudnative.model.EstadoGuia;
import cl.duoc.cloudnative.model.GuiaDespacho;
import java.time.Instant;

public record GuiaResponse(
        Long idGuia,
        String transportista,
        String fecha,
        EstadoGuia estado,
        String efsPath,
        String s3Key,
        long tamanioBytes,
        Instant timestamp
) {
    public static GuiaResponse from(GuiaDespacho guia) {
        return new GuiaResponse(
                guia.getIdGuia(),
                guia.getTransportista().getAlias(),
                guia.getFecha(),
                guia.getEstado(),
                guia.getEfsPath(),
                guia.getS3Key(),
                guia.getTamanioBytes(),
                guia.getTimestamp()
        );
    }
}
