package de.barriereblick.api.user.dto;

import java.util.UUID;

/**
 * Antwort fuer GET /api/me. Bewusst OHNE password_hash und OHNE stripe_customer_id.
 */
public record MeResponse(UserPart user, OrganizationPart organization) {

    public record UserPart(UUID id, String email, String role) {
    }

    public record OrganizationPart(UUID id, String name, String logoUrl, String brandColor, String plan) {
    }
}
