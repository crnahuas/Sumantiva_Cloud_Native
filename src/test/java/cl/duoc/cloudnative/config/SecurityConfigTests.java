package cl.duoc.cloudnative.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.cloudnative.dto.GuiaRequest;
import cl.duoc.cloudnative.dto.GuiaResponse;
import cl.duoc.cloudnative.model.EstadoGuia;
import cl.duoc.cloudnative.service.GuiaService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Test
    void claimExtensionConsultaRoleSeConvierteEnAutoridad() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("extension_consultaRole", "DESCARGA_GUIAS")
                .build();

        AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("DESCARGA_GUIAS");
    }

    @Test
    void sinTokenNoPuedeConsultarGuias() throws Exception {
        mockMvc.perform(get("/api/guias")
                        .param("transportista", "transportistaX")
                        .param("fecha", "202606"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gestorGuiasPuedeCrearGuias() throws Exception {
        mockMvc.perform(post("/api/guias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("GESTOR_GUIAS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transportista": "transportistaX",
                                  "credencialDescarga": "clave123",
                                  "fecha": "202606",
                                  "contenido": "Pedido de prueba"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void descargaGuiasNoPuedeCrearGuias() throws Exception {
        mockMvc.perform(post("/api/guias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("DESCARGA_GUIAS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transportista": "transportistaX",
                                  "credencialDescarga": "clave123",
                                  "fecha": "202606",
                                  "contenido": "Pedido de prueba"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void descargaGuiasPuedeDescargarGuias() throws Exception {
        mockMvc.perform(post("/api/guias/2/descarga")
                        .with(jwt().authorities(new SimpleGrantedAuthority("DESCARGA_GUIAS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credencialDescarga": "clave123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void gestorGuiasNoPuedeDescargarGuias() throws Exception {
        mockMvc.perform(post("/api/guias/2/descarga")
                        .with(jwt().authorities(new SimpleGrantedAuthority("GESTOR_GUIAS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "credencialDescarga": "clave123"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorGuiasPuedeConsultarGuias() throws Exception {
        mockMvc.perform(get("/api/guias")
                        .with(jwt().authorities(new SimpleGrantedAuthority("GESTOR_GUIAS")))
                        .param("transportista", "transportistaX")
                        .param("fecha", "202606"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class FakeGuiaServiceConfig {

        @Bean
        @Primary
        GuiaService guiaService() {
            return new FakeGuiaService();
        }
    }

    private static class FakeGuiaService extends GuiaService {

        FakeGuiaService() {
            super(null, null, null, null);
        }

        @Override
        public GuiaResponse crear(GuiaRequest request) {
            return response(EstadoGuia.CREADA);
        }

        @Override
        public byte[] descargar(Long idGuia, String credencialDescarga) {
            return "pdf".getBytes();
        }

        @Override
        public List<GuiaResponse> consultar(String transportista, String fecha) {
            return List.of(response(EstadoGuia.CREADA));
        }
    }

    private static GuiaResponse response(EstadoGuia estado) {
        return new GuiaResponse(
                2L,
                "transportistaX",
                "202606",
                estado,
                "/app/efs/202606/transportistaX/guia2.pdf",
                null,
                189,
                Instant.parse("2026-06-29T20:52:17Z")
        );
    }
}
