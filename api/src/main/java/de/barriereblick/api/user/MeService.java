package de.barriereblick.api.user;

import de.barriereblick.api.org.Organization;
import de.barriereblick.api.user.dto.MeResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MeService {

    private final UserRepository userRepository;

    public MeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Laedt den User ueber die userId aus dem validierten JWT (sub) und die
     * Organisation ueber die DB-Beziehung des Users – NICHT aus dem orgId-Claim.
     * Org-Scoping bleibt damit serverseitig verifiziert (Muster fuer alle M1+-Endpoints).
     */
    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        AppUser user = userRepository.findById(userId)
                // User aus gueltigem Token existiert nicht mehr -> wie nicht authentifiziert behandeln
                .orElseThrow(() -> new BadCredentialsException("Unbekannter Benutzer"));

        Organization organization = user.getOrganization();
        return new MeResponse(
                new MeResponse.UserPart(user.getId(), user.getEmail(), user.getRole().name()),
                new MeResponse.OrganizationPart(
                        organization.getId(),
                        organization.getName(),
                        organization.getLogoUrl(),
                        organization.getBrandColor(),
                        organization.getPlan().name()));
    }
}
