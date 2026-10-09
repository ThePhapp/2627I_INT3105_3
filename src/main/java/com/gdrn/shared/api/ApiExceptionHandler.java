package com.gdrn.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    // A 415 can occur before MVC chooses a controller, so scoped advice cannot handle it.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> media(HttpServletRequest request) {
        return ResponseEntity.status(415).cacheControl(CacheControl.noStore())
                .body(ApiError.of(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json.", request.getRequestURI()));
    }
}
