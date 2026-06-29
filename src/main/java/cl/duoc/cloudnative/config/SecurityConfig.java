package cl.duoc.cloudnative.config;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String DEFAULT_ISSUER = "https://duocucazure.b2clogin.com/tfp/f1ef6dd7-1653-4742-be87-71512d709704/b2c_1_duocdemoazure_registro_login/v2.0/";
    private static final String DEFAULT_JWKS_URI = "https://duocucazure.b2clogin.com/duocucazure.onmicrosoft.com/b2c_1_duocdemoazure_registro_login/discovery/v2.0/keys";

    private final String roleClaim;

    public SecurityConfig(@Value("${app.security.role-claim:extension_consultaRole}") String roleClaim) {
        this.roleClaim = roleClaim;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/guias/*/descarga").hasAuthority("DESCARGA_GUIAS")
                        .requestMatchers("/api/guias/**").hasAuthority("GESTOR_GUIAS")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:" + DEFAULT_ISSUER + "}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:" + DEFAULT_JWKS_URI + "}") String jwksUri
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromRoleClaim);
        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFromRoleClaim(Jwt jwt) {
        Object claim = jwt.getClaims().get(roleClaim);
        if (claim instanceof String role && !role.isBlank()) {
            return List.of(new SimpleGrantedAuthority(role));
        }
        if (claim instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !role.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        }
        return List.of();
    }
}
