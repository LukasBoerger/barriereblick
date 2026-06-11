package de.barriereblick.api.auth;

import de.barriereblick.api.auth.dto.TokenResponse;
import de.barriereblick.api.user.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Erzeugt JWTs (HS256): sub = userId, Claims orgId + role, Expiry konfigurierbar
 * ueber app.jwt.expiry. Validierung uebernimmt der Spring Resource Server.
 */
@Service
public class TokenService {

    public static final String ISSUER = "barriereblick";

    private final JwtEncoder jwtEncoder;
    private final Duration expiry;

    public TokenService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiry}") Duration expiry) {
        this.jwtEncoder = jwtEncoder;
        this.expiry = expiry;
    }

    public TokenResponse issueToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiry);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("orgId", user.getOrganization().getId().toString())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, expiresAt);
    }
}
