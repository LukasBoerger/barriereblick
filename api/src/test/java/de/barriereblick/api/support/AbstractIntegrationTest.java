package de.barriereblick.api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basis fuer alle Integrationstests: ein gemeinsamer Postgres-Testcontainer
 * (Singleton-Pattern, {@code @ServiceConnection}) + MockMvc gegen den vollen
 * Spring-Kontext. Flyway laeuft beim Kontextstart gegen die LEERE Container-DB
 * (ddl-auto: validate) – damit ist das Akzeptanzkriterium "Migration laeuft auf
 * leerer Postgres-DB durch" Voraussetzung fuer JEDEN dieser Tests.
 *
 * <p>Der Container wird bewusst NICHT pro Testklasse gestoppt (Singleton +
 * Spring-Context-Cache); Testcontainers/Ryuk raeumt am JVM-Ende auf.
 */
@SpringBootTest(properties = {
        // Test-Secret (>= 32 Bytes) und feste CORS-Origin, unabhaengig von Profil-Defaults
        "app.jwt.secret=integration-test-secret-mindestens-32-bytes-lang",
        "app.cors.frontend-origin=http://localhost:4200"
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String FRONTEND_ORIGIN = "http://localhost:4200";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Testisolation: Container + Spring-Kontext werden geteilt, Daten nicht.
        jdbcTemplate.execute("DELETE FROM app_user");
        jdbcTemplate.execute("DELETE FROM organization");
    }

    protected MvcResult register(String orgName, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orgName": "%s", "email": "%s", "password": "%s"}
                                """.formatted(orgName, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    protected String registerAndGetToken(String orgName, String email, String password) throws Exception {
        return readToken(register(orgName, email, password));
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readToken(result);
    }

    private String readToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
