package de.barriereblick.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "E-Mail-Adresse darf nicht leer sein.")
        String email,

        @NotBlank(message = "Passwort darf nicht leer sein.")
        String password) {
}
