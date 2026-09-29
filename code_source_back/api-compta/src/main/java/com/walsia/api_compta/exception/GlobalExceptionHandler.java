package com.walsia.api_compta.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<Map<String, Object>> gererRessourceIntrouvable(RessourceIntrouvableException e) {
        return corpsErreur(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<Map<String, Object>> gererConflit(ConflitException e) {
        return corpsErreur(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(TokenInvalideException.class)
    public ResponseEntity<Map<String, Object>> gererTokenInvalide(TokenInvalideException e) {
        return corpsErreur(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AuthentificationEchoueeException.class)
    public ResponseEntity<Map<String, Object>> gererAuthentificationEchouee(AuthentificationEchoueeException e) {
        return corpsErreur(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> gererValidation(MethodArgumentNotValidException e) {
        Map<String, String> erreursParChamp = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError ->
                erreursParChamp.put(fieldError.getField(), fieldError.getDefaultMessage()));

        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("timestamp", Instant.now().toString());
        corps.put("status", HttpStatus.BAD_REQUEST.value());
        corps.put("message", "Erreur de validation");
        corps.put("erreurs", erreursParChamp);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corps);
    }

    private ResponseEntity<Map<String, Object>> corpsErreur(HttpStatus status, String message) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("timestamp", Instant.now().toString());
        corps.put("status", status.value());
        corps.put("message", message);
        return ResponseEntity.status(status).body(corps);
    }
}
