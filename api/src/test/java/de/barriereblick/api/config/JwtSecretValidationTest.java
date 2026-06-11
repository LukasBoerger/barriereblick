package de.barriereblick.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Startup-Validierung des JWT-Secrets (ohne Container, ohne volle App):
 * fehlendes oder zu kurzes JWT_SECRET verhindert den Kontextstart mit
 * klarer Meldung – kein stiller Betrieb mit schwachem Secret.
 */
class JwtSecretValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class);

    @Test
    @DisplayName("Kontextstart schlaegt fehl, wenn JWT_SECRET fehlt")
    void contextFails_whenSecretIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_SECRET");
        });
    }

    @Test
    @DisplayName("Kontextstart schlaegt fehl, wenn JWT_SECRET leer ist")
    void contextFails_whenSecretIsBlank() {
        contextRunner
                .withPropertyValues("app.jwt.secret=   ")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("JWT_SECRET");
                });
    }

    @Test
    @DisplayName("Kontextstart schlaegt fehl, wenn JWT_SECRET kuerzer als 32 Bytes ist")
    void contextFails_whenSecretIsTooShort() {
        contextRunner
                // 31 Bytes -> unter der 256-Bit-Grenze fuer HS256
                .withPropertyValues("app.jwt.secret=" + "x".repeat(31))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("32 Bytes");
                });
    }

    @Test
    @DisplayName("Kontextstart gelingt mit 32-Byte-Secret; Encoder/Decoder-Beans vorhanden")
    void contextStarts_withValidSecret() {
        contextRunner
                .withPropertyValues("app.jwt.secret=" + "x".repeat(32))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);
                    assertThat(context).hasSingleBean(JwtEncoder.class);
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                });
    }
}
