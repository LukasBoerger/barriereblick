package de.barriereblick.api.config;

import de.barriereblick.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Akzeptanzkriterium: Flyway-Migration laeuft auf leerer Postgres-DB durch.
 * Der Kontextstart selbst beweist das bereits (Flyway + ddl-auto: validate);
 * hier zusaetzlich explizit: V1 erfolgreich, genau die zwei Tabellen aus dem
 * Task-Scope, Unique-Index auf lower(email).
 */
class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V1__init ist erfolgreich auf der leeren Container-DB gelaufen")
    void v1Migration_succeeded() {
        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("Schema enthaelt GENAU die zwei Task-Tabellen organization und app_user")
    void schema_containsExactlyTheTwoScopedTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history' "
                        + "ORDER BY table_name",
                String.class);
        // "Nicht in Scope": keine site/scan_run/finding-Tabellen in M0
        assertThat(tables).containsExactly("app_user", "organization");
    }

    @Test
    @DisplayName("Unique-Index auf lower(email) existiert (case-insensitive E-Mail-Eindeutigkeit)")
    void uniqueEmailIndex_exists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes "
                        + "WHERE tablename = 'app_user' AND indexname = 'ux_app_user_email'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
