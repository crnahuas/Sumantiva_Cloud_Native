package cl.duoc.cloudnative.controller;

import cl.duoc.cloudnative.dto.GuiaRequest;
import cl.duoc.cloudnative.dto.GuiaResponse;
import cl.duoc.cloudnative.dto.PermisoDescargaRequest;
import cl.duoc.cloudnative.service.GuiaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    @PostMapping
    public ResponseEntity<GuiaResponse> crear(@Valid @RequestBody GuiaRequest request) {
        return ResponseEntity.ok(guiaService.crear(request));
    }

    @PostMapping("/{idGuia}/s3")
    public ResponseEntity<GuiaResponse> subirAS3(@PathVariable Long idGuia) {
        return ResponseEntity.ok(guiaService.subirAS3(idGuia));
    }

    @PostMapping("/{idGuia}/descarga")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long idGuia,
            @Valid @RequestBody PermisoDescargaRequest request
    ) {
        byte[] file = guiaService.descargar(idGuia, request.credencialDescarga());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("guia" + idGuia + ".pdf")
                        .build()
                        .toString())
                .body(file);
    }

    @PutMapping("/{idGuia}")
    public ResponseEntity<GuiaResponse> actualizar(
            @PathVariable Long idGuia,
            @Valid @RequestBody GuiaRequest request
    ) {
        return ResponseEntity.ok(guiaService.actualizar(idGuia, request));
    }

    @DeleteMapping("/{idGuia}")
    public ResponseEntity<GuiaResponse> eliminar(@PathVariable Long idGuia) {
        return ResponseEntity.ok(guiaService.eliminar(idGuia));
    }

    @GetMapping
    public ResponseEntity<List<GuiaResponse>> consultar(
            @RequestParam String transportista,
            @RequestParam @Pattern(regexp = "\\d{6}", message = "fecha debe usar formato YYYYMM") String fecha
    ) {
        return ResponseEntity.ok(guiaService.consultar(transportista, fecha));
    }
}
