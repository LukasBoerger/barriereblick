package de.barriereblick.api.config;

import de.barriereblick.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Akzeptanzkriterium Default-Deny: kein Endpoint ausser /api/auth/** ist ohne
 * Token erreichbar. Plus: API-Fehlervertrag (400 mit Feldfehlern, 409 bei
 * Duplikat-E-Mail) und CORS nur fuer die konfigurierte Frontend-Origin.
 */
class SecurityConfigIT extends AbstractIntegrationTest {

    private static final String VALID_PASSWORD = "superlanges-passwort";

    // --- Default-Deny -----------------------------------------------------

    @ParameterizedTest(name = "GET {0} ohne Token -> 401")
    @ValueSource(strings = {"/api/me", "/api/sites", "/api/gibt-es-nicht", "/api/runs/123/findings"})
    @DisplayName("beliebiger Pfad unter /api/** ausser /api/auth/** liefert ohne Token 401 (Default-Deny)")
    void anyApiPath_withoutToken_returns401(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("auch POST auf unbekannte /api-Pfade liefert ohne Token 401, nicht 404")
    void unknownApiPath_post_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/sites").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- /api/auth/** ist ohne Token erreichbar ---------------------------

    @Test
    @DisplayName("POST /api/auth/register ist ohne Token erreichbar (201)")
    void register_withoutToken_isReachable() throws Exception {
        register("Agentur Offen", "offen@agentur.de", VALID_PASSWORD);
    }

    @Test
    @DisplayName("POST /api/auth/login ist ohne Token erreichbar (kein 401 wegen fehlendem Token, sondern 401 wegen Credentials)")
    void login_withoutToken_isReachable() throws Exception {
        // Erreichbarkeit zeigt sich daran, dass der Request fachlich beantwortet
        // wird (generische Credential-Meldung), nicht vom Security-Filter geblockt.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "niemand@agentur.de", "password": "falsches-passwort"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-Mail oder Passwort ist falsch."));
    }

    // --- Validierungsfehler -> 400 mit Feldfehlern ------------------------

    @Test
    @DisplayName("register mit invalider E-Mail und zu kurzem Passwort liefert 400 mit Feldfehlern")
    void register_invalidEmailAndShortPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orgName": "Agentur", "email": "keine-email", "password": "kurz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    @DisplayName("register mit leerem orgName liefert 400 mit Feldfehler")
    void register_blankOrgName_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orgName": " ", "email": "anna@agentur.de", "password": "superlanges-passwort"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.orgName").isNotEmpty());
    }

    @Test
    @DisplayName("register mit Passwort ueber 72 UTF-8-Bytes (Mehrbyte-Zeichen) liefert 400, nicht 500")
    void register_passwordOver72Utf8Bytes_returns400() throws Exception {
        // 40 Umlaute: 40 Zeichen (besteht @Size max 72), aber 80 UTF-8-Bytes (BCrypt-Limit 72).
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orgName": "Agentur", "email": "bytes@agentur.de", "password": "%s"}
                                """.formatted("ä".repeat(40))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    // --- Duplikat-E-Mail -> 409 -------------------------------------------

    @Test
    @DisplayName("register mit bereits verwendeter E-Mail liefert 409 (auch bei anderer Schreibweise)")
    void register_duplicateEmail_returns409() throws Exception {
        register("Agentur Eins", "doppelt@agentur.de", VALID_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orgName": "Agentur Zwei", "email": "Doppelt@Agentur.DE", "password": "anderes-langes-passwort"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // --- CORS ---------------------------------------------------------------

    @Test
    @DisplayName("CORS-Preflight fuer die konfigurierte Frontend-Origin wird erlaubt")
    void corsPreflight_allowedOrigin_succeeds() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN));
    }

    @Test
    @DisplayName("CORS-Preflight fuer fremde Origin wird abgelehnt (kein Wildcard)")
    void corsPreflight_unknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://boese-seite.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
