package de.barriereblick.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import de.barriereblick.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Akzeptanzkriterium Org-Scoping: User von Org A sieht auf /api/me ausschliesslich
 * Daten der eigenen Organisation, niemals Daten von Org B.
 * Vorbereitender Test – das Muster gilt fuer JEDEN neuen Endpoint ab M1.
 */
class OrgScopingIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Token von User A liefert auf /api/me nur Org-A-Daten, nie Org-B-Daten")
    void me_userOfOrgA_seesOnlyOrgAData() throws Exception {
        String tokenA = registerAndGetToken("Agentur Alpha", "a@alpha.de", "passwort-org-alpha");
        String tokenB = registerAndGetToken("Agentur Beta", "b@beta.de", "passwort-org-beta!");

        JsonNode meA = fetchMe(tokenA);
        JsonNode meB = fetchMe(tokenB);

        // User A: explizit die eigenen Felder
        assertThat(meA.at("/user/email").asText()).isEqualTo("a@alpha.de");
        assertThat(meA.at("/organization/name").asText()).isEqualTo("Agentur Alpha");

        // User B: explizit die eigenen Felder
        assertThat(meB.at("/user/email").asText()).isEqualTo("b@beta.de");
        assertThat(meB.at("/organization/name").asText()).isEqualTo("Agentur Beta");

        // Strikte Trennung: keine IDs oder Namen der jeweils anderen Organisation
        String orgIdA = meA.at("/organization/id").asText();
        String orgIdB = meB.at("/organization/id").asText();
        assertThat(orgIdA).isNotBlank().isNotEqualTo(orgIdB);
        assertThat(meA.at("/user/id").asText()).isNotEqualTo(meB.at("/user/id").asText());
        assertThat(meA.toString())
                .doesNotContain(orgIdB)
                .doesNotContain("Agentur Beta")
                .doesNotContain("b@beta.de");
        assertThat(meB.toString())
                .doesNotContain(orgIdA)
                .doesNotContain("Agentur Alpha")
                .doesNotContain("a@alpha.de");
    }

    private JsonNode fetchMe(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
