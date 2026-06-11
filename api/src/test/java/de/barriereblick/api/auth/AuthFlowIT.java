package de.barriereblick.api.auth;

import de.barriereblick.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Akzeptanzkriterium: register -> login -> GET /api/me mit Token = 200,
 * ohne Token = 401, manipulierter/ungueltiger/abgelaufener Token = 401.
 */
class AuthFlowIT extends AbstractIntegrationTest {

    private static final String EMAIL = "anna@agentur.de";
    private static final String PASSWORD = "superlanges-passwort";

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("register -> login -> GET /api/me mit Token liefert 200 mit User- und Org-Daten")
    void fullAuthFlow_registerLoginMe_returnsUserAndOrganization() throws Exception {
        MvcResult registerResult = register("Agentur Beispiel", EMAIL, PASSWORD);
        assertThat(registerResult.getResponse().getContentAsString()).contains("token");

        String token = login(EMAIL, PASSWORD);
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.organization.id").isNotEmpty())
                .andExpect(jsonPath("$.organization.name").value("Agentur Beispiel"))
                .andExpect(jsonPath("$.organization.plan").value("FREE"))
                // Sensible Felder duerfen die API NIE verlassen
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.organization.stripeCustomerId").doesNotExist());
    }

    @Test
    @DisplayName("register normalisiert die E-Mail: Login funktioniert case-insensitiv")
    void register_thenLoginWithDifferentCasing_succeeds() throws Exception {
        register("Agentur Beispiel", "  Anna@Agentur.DE ", PASSWORD);

        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    @DisplayName("GET /api/me ohne Token liefert 401 mit generischem ApiError")
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/me mit syntaktisch ungueltigem Token liefert 401")
    void me_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer kein-echtes-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/me mit manipuliertem Payload (Signatur ungueltig) liefert 401")
    void me_withTamperedToken_returns401() throws Exception {
        String token = registerAndGetToken("Agentur Beispiel", EMAIL, PASSWORD);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tamperPayload(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/me mit abgelaufenem Token liefert 401")
    void me_withExpiredToken_returns401() throws Exception {
        register("Agentur Beispiel", EMAIL, PASSWORD);

        // Mit demselben Secret signiert, aber exp deutlich in der Vergangenheit
        // (Default-Clock-Skew des Decoders ist 60 s).
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("barriereblick")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(600))
                .claim("orgId", UUID.randomUUID().toString())
                .claim("role", "ADMIN")
                .build();
        String expiredToken = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Ersetzt im JWT-Payload die Rolle, ohne neu zu signieren –
     * die Signatur passt danach nicht mehr zum Inhalt.
     */
    private static String tamperPayload(String token) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tampered = payload.replace("\"role\":\"ADMIN\"", "\"role\":\"SUPERADMIN\"");
        assertThat(tampered).isNotEqualTo(payload);
        parts[1] = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tampered.getBytes(StandardCharsets.UTF_8));
        return String.join(".", parts);
    }
}
