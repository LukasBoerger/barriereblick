package de.barriereblick.api.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import de.barriereblick.api.org.Organization;
import de.barriereblick.api.user.AppUser;
import de.barriereblick.api.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TokenServiceTest {

    private static final String TEST_SECRET = "test-secret-fuer-unit-tests-mind-32-bytes-lang";
    private static final Duration EXPIRY = Duration.ofHours(24);

    private TokenService tokenService;
    private NimbusJwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        tokenService = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)), EXPIRY);
        jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    @DisplayName("issueToken setzt sub, orgId, role, Issuer und Expiry korrekt")
    void issueToken_setsClaimsAndExpiry() throws Exception {
        var organization = new Organization("Agentur");
        var orgId = UUID.randomUUID();
        setId(organization, orgId);

        var user = new AppUser(organization, "anna@agentur.de", "$2a$10$hash", UserRole.ADMIN);
        var userId = UUID.randomUUID();
        setId(user, userId);

        var response = tokenService.issueToken(user);

        Jwt jwt = jwtDecoder.decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("orgId")).isEqualTo(orgId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(TokenService.ISSUER);

        assertThat(response.expiresAt())
                .isCloseTo(Instant.now().plus(EXPIRY), within(5, ChronoUnit.SECONDS));
        assertThat(jwt.getExpiresAt()).isCloseTo(response.expiresAt(), within(1, ChronoUnit.SECONDS));
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
