package de.barriereblick.api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import de.barriereblick.api.auth.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Stellt JwtEncoder/JwtDecoder aus dem symmetrischen Secret (HS256) bereit.
 * Startup-Validierung: ohne JWT_SECRET bzw. mit einem Secret unter 256 Bit
 * startet die Anwendung NICHT.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    static final int MIN_SECRET_BYTES = 32; // 256 Bit fuer HS256

    // Marker im eingecheckten local-Dev-Default-Secret (siehe application.yml).
    static final String DEV_SECRET_MARKER = "dev-only";

    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET ist nicht gesetzt. Die Anwendung startet nicht ohne JWT-Secret "
                            + "(Env-Variable JWT_SECRET, mindestens 32 Bytes / 256 Bit).");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET ist zu kurz: " + keyBytes.length + " Bytes. "
                            + "Erforderlich sind mindestens 32 Bytes (256 Bit) fuer HS256.");
        }
        if (secret.contains(DEV_SECRET_MARKER)) {
            log.warn("JWT: Das eingecheckte DEV-Secret ist aktiv. "
                    + "Nur fuer lokale Entwicklung zulaessig – NIEMALS in Produktion verwenden.");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Zusaetzlich zur Signatur/Expiry auch den Issuer validieren – Tokens
        // anderer Aussteller (z. B. bei versehentlich geteiltem Secret) sind ungueltig.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
        return decoder;
    }
}
