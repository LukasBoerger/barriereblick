package de.barriereblick.api.auth.dto;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt) {
}
