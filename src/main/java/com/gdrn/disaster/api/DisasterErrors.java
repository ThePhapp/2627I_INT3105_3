package com.gdrn.disaster.api;

import com.gdrn.disaster.application.*;
import com.gdrn.disaster.domain.InvalidDisasterTransition;
import com.gdrn.disaster.domain.StaleDisasterVersion;
import com.gdrn.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice(assignableTypes = DisasterController.class)
public class DisasterErrors {
    @ExceptionHandler({InvalidDisasterInput.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception error, HttpServletRequest request) {
        String field = error instanceof InvalidDisasterInput input ? input.field() : "body";
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(new ApiError(
                Instant.now(), 400, "VALIDATION_ERROR", "Invalid request.", request.getRequestURI(),
                List.of(new ApiError.FieldError(field, "Invalid " + field + "."))));
    }

    @ExceptionHandler(DisasterNotFound.class)
    ResponseEntity<ApiError> missing(HttpServletRequest request) {
        return problem(404, "NOT_FOUND", "Disaster not found.", request);
    }

    @ExceptionHandler(StaleDisasterVersion.class)
    ResponseEntity<ApiError> stale(HttpServletRequest request) {
        return problem(409, "STALE_VERSION", "Disaster version is stale.", request);
    }

    @ExceptionHandler(InvalidDisasterTransition.class)
    ResponseEntity<ApiError> transition(HttpServletRequest request) {
        return problem(409, "INVALID_TRANSITION", "Disaster transition is not allowed.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> media(HttpServletRequest request) {
        return problem(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(HttpServletRequest request) {
        return problem(500, "INTERNAL_ERROR", "Request could not be completed.", request);
    }

    private static ResponseEntity<ApiError> problem(int status, String code, String message,
                                                     HttpServletRequest request) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
