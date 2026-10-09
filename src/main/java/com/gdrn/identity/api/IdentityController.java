package com.gdrn.identity.api;

import com.gdrn.identity.application.IdentityService;
import com.gdrn.identity.application.InvalidIdentityInput;
import com.gdrn.identity.domain.Role;
import com.gdrn.identity.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class IdentityController {
    private final IdentityService service;
    public IdentityController(IdentityService service) { this.service = service; }

    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    @Operation(operationId = "E01", summary = "Sign in")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        rejectQuery(request);
        var result = service.login(body.email(), body.password());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new LoginResponse(
                result.token().value(), "Bearer", result.token().expiresIn(), result.token().expiresAt(), UserResponse.from(result.user())));
    }

    @GetMapping(value = "/me", produces = "application/json")
    @Operation(operationId = "E02", summary = "Current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal, HttpServletRequest request) {
        rejectQuery(request);
        if (request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null) {
            throw new InvalidIdentityInput("body");
        }
        User user = service.me(UUID.fromString(principal.getSubject()), Role.valueOf(principal.getClaimAsString("role")));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(UserResponse.from(user));
    }

    private static void rejectQuery(HttpServletRequest request) {
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            throw new InvalidIdentityInput("query");
        }
    }
    public record LoginRequest(String email, String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }
    public record UserResponse(UUID id, String email, Role role) {
        static UserResponse from(User user) { return new UserResponse(user.id(), user.emailAddress().value(), user.role()); }
    }
    public record LoginResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt, UserResponse user) {
        @Override public String toString() { return "LoginResponse[redacted]"; }
    }
}
