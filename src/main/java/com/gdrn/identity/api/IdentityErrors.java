package com.gdrn.identity.api;

import com.gdrn.identity.application.InvalidCredentials;
import com.gdrn.identity.application.InvalidIdentityInput;
import com.gdrn.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice(assignableTypes = IdentityController.class)
public class IdentityErrors {
    @ExceptionHandler(InvalidCredentials.class)
    ResponseEntity<ApiError> credentials(HttpServletRequest request) {
        boolean login = request.getRequestURI().equals("/api/auth/login");
        return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
                .headers(headers -> { if (!login) headers.set("WWW-Authenticate", "Bearer"); })
                .body(ApiError.of(401, login ? "INVALID_CREDENTIALS" : "UNAUTHENTICATED",
                        login ? "Invalid email or password." : "Authentication required.", request.getRequestURI()));
    }
    @ExceptionHandler({InvalidIdentityInput.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception error, HttpServletRequest request) {
        String field = error instanceof InvalidIdentityInput input ? input.field() : "body";
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(new ApiError(
                Instant.now(), 400, "VALIDATION_ERROR", "Invalid request.", request.getRequestURI(),
                List.of(new ApiError.FieldError(field, "Invalid " + field + "."))));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> media(HttpServletRequest request) {
        return ResponseEntity.status(415).cacheControl(CacheControl.noStore())
                .body(ApiError.of(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json.", request.getRequestURI()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(HttpServletRequest request) {
        return ResponseEntity.internalServerError().cacheControl(CacheControl.noStore())
                .body(ApiError.of(500, "INTERNAL_ERROR", "Request could not be completed.", request.getRequestURI()));
    }
}
