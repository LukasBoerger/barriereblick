package de.barriereblick.api.common;

/**
 * Fachliche Exception: Registrierung mit bereits verwendeter E-Mail-Adresse (HTTP 409).
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Diese E-Mail-Adresse wird bereits verwendet.");
    }
}
