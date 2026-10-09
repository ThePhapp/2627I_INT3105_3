package com.gdrn.shared.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdrn.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public final class SecurityErrorWriter {
    private final ObjectMapper mapper;
    public SecurityErrorWriter(ObjectMapper mapper) { this.mapper = mapper; }
    public void write(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(response.getOutputStream(), ApiError.of(status,
                status == 401 ? "UNAUTHENTICATED" : "FORBIDDEN",
                status == 401 ? "Authentication required." : "Access denied.", request.getRequestURI()));
    }
}
