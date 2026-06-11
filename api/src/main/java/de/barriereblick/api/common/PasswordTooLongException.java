package de.barriereblick.api.common;

/**
 * Passwort ueberschreitet das BCrypt-Limit von 72 Bytes (UTF-8).
 * Bean-Validation (@Size) zaehlt Zeichen, BCrypt zaehlt Bytes – diese Exception
 * deckt den Mehrbyte-Fall ab und wird zentral auf 400 gemappt.
 */
public class PasswordTooLongException extends RuntimeException {

    public PasswordTooLongException() {
        super("Passwort darf höchstens 72 Bytes (UTF-8) lang sein.");
    }
}
