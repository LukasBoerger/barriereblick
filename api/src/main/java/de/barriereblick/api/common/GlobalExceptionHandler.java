package de.barriereblick.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Fehlerbehandlung: keine technischen Exceptions ans Frontend.
 * Details werden serverseitig geloggt, die API liefert generische Meldungen.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "Eingaben ungültig.", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST.value(), "Anfrage konnte nicht gelesen werden.");
    }

    @ExceptionHandler(PasswordTooLongException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handlePasswordTooLong(PasswordTooLongException ex) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "Eingaben ungültig.",
                Map.of("password", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return ApiError.of(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleBadCredentials(BadCredentialsException ex) {
        // Generische Meldung: identisch fuer unbekannte E-Mail und falsches Passwort
        // (keine User-Enumeration ueber den Login).
        return ApiError.of(HttpStatus.UNAUTHORIZED.value(), "E-Mail oder Passwort ist falsch.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NoResourceFoundException ex) {
        return ApiError.of(HttpStatus.NOT_FOUND.value(), "Ressource nicht gefunden.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception ex) {
        log.error("Unerwarteter Fehler bei der Anfrageverarbeitung", ex);
        return ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Interner Fehler. Bitte später erneut versuchen.");
    }
}
