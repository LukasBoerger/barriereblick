package de.barriereblick.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Name der Organisation darf nicht leer sein.")
        @Size(max = 255, message = "Name der Organisation darf höchstens 255 Zeichen lang sein.")
        String orgName,

        @NotBlank(message = "E-Mail-Adresse darf nicht leer sein.")
        @Email(message = "E-Mail-Adresse ist ungültig.")
        @Size(max = 320, message = "E-Mail-Adresse darf höchstens 320 Zeichen lang sein.")
        String email,

        @NotBlank(message = "Passwort darf nicht leer sein.")
        @Size(min = 12, max = 72, message = "Passwort muss zwischen 12 und 72 Zeichen lang sein.")
        String password) {

    public RegisterRequest {
        // Whitespace vor der Bean-Validation entfernen, damit @Email gepaddete,
        // sonst gültige Adressen nicht ablehnt (Service normalisiert zusätzlich lowercase).
        email = email == null ? null : email.trim();
    }
}
