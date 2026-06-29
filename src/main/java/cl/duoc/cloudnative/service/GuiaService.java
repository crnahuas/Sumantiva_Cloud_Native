package cl.duoc.cloudnative.service;

import cl.duoc.cloudnative.config.StorageProperties;
import cl.duoc.cloudnative.dto.GuiaRequest;
import cl.duoc.cloudnative.dto.GuiaResponse;
import cl.duoc.cloudnative.exception.PermisoDenegadoException;
import cl.duoc.cloudnative.exception.RecursoNoEncontradoException;
import cl.duoc.cloudnative.model.EstadoGuia;
import cl.duoc.cloudnative.model.GuiaDespacho;
import cl.duoc.cloudnative.model.Transportista;
import cl.duoc.cloudnative.repository.GuiaDespachoRepository;
import cl.duoc.cloudnative.repository.TransportistaRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuiaService {

    private final GuiaDespachoRepository guiaRepository;
    private final TransportistaRepository transportistaRepository;
    private final StorageProperties storageProperties;
    private final S3StorageService s3StorageService;

    public GuiaService(
            GuiaDespachoRepository guiaRepository,
            TransportistaRepository transportistaRepository,
            StorageProperties storageProperties,
            S3StorageService s3StorageService
    ) {
        this.guiaRepository = guiaRepository;
        this.transportistaRepository = transportistaRepository;
        this.storageProperties = storageProperties;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public GuiaResponse crear(GuiaRequest request) {
        Transportista transportista = transportistaRepository.findByAlias(request.transportista())
                .orElseGet(() -> transportistaRepository.save(
                        new Transportista(request.transportista(), request.credencialDescarga())
                ));
        GuiaDespacho guia = guiaRepository.save(new GuiaDespacho(transportista, request.fecha(), "pendiente", 0));
        Path tempFile = writeTempFile(guia.getIdGuia(), request, "Guia creada");
        guia.setEfsPath(tempFile.toString());
        guia.setTamanioBytes(size(tempFile));
        return GuiaResponse.from(guiaRepository.save(guia));
    }

    @Transactional
    public GuiaResponse subirAS3(Long idGuia) {
        GuiaDespacho guia = getGuia(idGuia);
        Path path = Path.of(guia.getEfsPath());
        if (!Files.exists(path)) {
            throw new IllegalStateException("No existe el archivo temporal en EFS: " + path);
        }
        String key = buildS3Key(guia);
        s3StorageService.upload(path, key, guia.getTransportista().getAlias(), guia.getFecha());
        guia.setS3Key(key);
        guia.setEstado(EstadoGuia.SUBIDA_S3);
        guia.setTamanioBytes(size(path));
        return GuiaResponse.from(guiaRepository.save(guia));
    }

    @Transactional
    public GuiaResponse actualizar(Long idGuia, GuiaRequest request) {
        GuiaDespacho guia = getGuia(idGuia);
        if (!guia.getTransportista().getAlias().equals(request.transportista())) {
            throw new IllegalStateException("La guia solo puede actualizarse para el mismo transportista");
        }
        if (!guia.getFecha().equals(request.fecha())) {
            throw new IllegalStateException("La guia solo puede actualizarse para la misma fecha YYYYMM");
        }
        Path tempFile = writeTempFile(idGuia, request, "Guia actualizada");
        guia.setEfsPath(tempFile.toString());
        guia.setTamanioBytes(size(tempFile));
        guia.setEstado(EstadoGuia.ACTUALIZADA);
        String key = buildS3Key(guia);
        s3StorageService.upload(tempFile, key, guia.getTransportista().getAlias(), guia.getFecha());
        guia.setS3Key(key);
        return GuiaResponse.from(guiaRepository.save(guia));
    }

    @Transactional(readOnly = true)
    public byte[] descargar(Long idGuia, String credencialDescarga) {
        GuiaDespacho guia = getGuia(idGuia);
        validarPermiso(guia, credencialDescarga);
        if (guia.getS3Key() == null) {
            throw new IllegalStateException("La guia aun no fue subida a S3");
        }
        return s3StorageService.download(guia.getS3Key());
    }

    @Transactional
    public GuiaResponse eliminar(Long idGuia) {
        GuiaDespacho guia = getGuia(idGuia);
        if (guia.getS3Key() != null) {
            s3StorageService.delete(guia.getS3Key());
        }
        guia.setS3Key(null);
        guia.setEstado(EstadoGuia.ELIMINADA);
        return GuiaResponse.from(guiaRepository.save(guia));
    }

    @Transactional(readOnly = true)
    public List<GuiaResponse> consultar(String transportista, String fecha) {
        return guiaRepository
                .findByTransportistaAliasAndFechaAndEstadoNotOrderByTimestampDesc(
                        transportista,
                        fecha,
                        EstadoGuia.ELIMINADA
                )
                .stream()
                .map(GuiaResponse::from)
                .toList();
    }

    private GuiaDespacho getGuia(Long idGuia) {
        return guiaRepository.findById(idGuia)
                .orElseThrow(() -> new RecursoNoEncontradoException("Guia no encontrada: " + idGuia));
    }

    private void validarPermiso(GuiaDespacho guia, String credencialDescarga) {
        if (!guia.getTransportista().getCredencialDescarga().equals(credencialDescarga)) {
            throw new PermisoDenegadoException("Credencial de descarga no autorizada para el transportista");
        }
    }

    private Path writeTempFile(Long idGuia, GuiaRequest request, String accion) {
        try {
            Path directory = Path.of(storageProperties.getEfsPath(), request.fecha(), request.transportista());
            Files.createDirectories(directory);
            String fileName = "guia" + idGuia + ".pdf";
            Path file = directory.resolve(fileName);
            String content = """
                    %s
                    Transportista: %s
                    Fecha: %s
                    Contenido: %s
                    Timestamp: %s
                    """.formatted(accion, request.transportista(), request.fecha(), request.contenido(), Instant.now());
            Files.writeString(
                    file,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return file;
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible escribir la guia en EFS", ex);
        }
    }

    private String buildS3Key(GuiaDespacho guia) {
        return guia.getFecha() + "/" + guia.getTransportista().getAlias() + "/guia" + guia.getIdGuia() + ".pdf";
    }

    private long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible leer metadata del archivo", ex);
        }
    }
}
