package de.barriereblick.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Einheitliches Fehler-DTO der API – der Vertrag, auf den sich web/ verlaesst.
 * Enthaelt NIE technische Details (Stacktraces, SQL, interne Meldungen).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fieldErrors) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null);
    }
}
