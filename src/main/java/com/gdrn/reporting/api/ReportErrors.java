package com.gdrn.reporting.api;

import com.gdrn.reporting.application.ReportAccessDenied;
import com.gdrn.reporting.application.ReportNotFound;
import com.gdrn.reporting.domain.InvalidReport;
import com.gdrn.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReportController.class)
public class ReportErrors {
    @ExceptionHandler({InvalidReport.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception error, HttpServletRequest request) {
        String field = error instanceof InvalidReport invalid ? invalid.field() : "body";
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(new ApiError(
                Instant.now(), 400, "VALIDATION_ERROR", "Invalid request.", request.getRequestURI(),
                List.of(new ApiError.FieldError(field, "Invalid " + field + "."))));
    }

    @ExceptionHandler(ReportNotFound.class)
    ResponseEntity<ApiError> missing(HttpServletRequest request) {
        return error(404, "NOT_FOUND", "Resource not found.", request);
    }

    @ExceptionHandler(ReportAccessDenied.class)
    ResponseEntity<ApiError> forbidden(HttpServletRequest request) {
        return error(403, "FORBIDDEN", "Access denied.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(HttpServletRequest request) {
        return error(500, "INTERNAL_ERROR", "Request could not be completed.", request);
    }

    private ResponseEntity<ApiError> error(int status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
