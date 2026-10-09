package com.gdrn.identity.infrastructure.security;

import com.gdrn.identity.application.port.TokenIssuer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    @Bean Clock identityClock() { return Clock.systemUTC(); }

    @Bean SecretKey jwtSigningKey(@Value("${JWT_SECRET_BASE64:}") String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length < 32) throw new IllegalArgumentException();
            return new SecretKeySpec(bytes, "HmacSHA256");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must encode at least 32 random bytes");
        }
    }

    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean JwtDecoder jwtDecoder(SecretKey key, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator("gdrn"), jwt -> validateClaims(jwt, clock.instant())));
        return decoder;
    }

    private OAuth2TokenValidatorResult validateClaims(Jwt jwt, Instant now) {
        try {
            Instant issued = jwt.getIssuedAt();
            Instant notBefore = jwt.getNotBefore();
            Instant expiry = jwt.getExpiresAt();
            boolean valid = jwt.getSubject() != null && UUID_PATTERN.matcher(jwt.getSubject()).matches()
                    && jwt.getClaims().get("role") instanceof String
                    && jwt.getAudience() != null && jwt.getAudience().contains("gdrn-spa")
                    && issued != null && notBefore != null && expiry != null
                    && issued.equals(notBefore) && expiry.equals(issued.plusSeconds(900))
                    && !now.isBefore(notBefore) && now.isBefore(expiry);
            if (valid) return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException ignored) {
            // Untrusted claims must produce a generic authentication failure, never a 500.
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
    }

    @Bean TokenIssuer tokenIssuer(JwtEncoder encoder, Clock clock) {
        return user -> {
            Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
            var claims = JwtClaimsSet.builder().issuer("gdrn").audience(List.of("gdrn-spa"))
                    .subject(user.id().toString()).claim("role", user.role().name())
                    .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(900)).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            return new TokenIssuer.IssuedToken(token, now.plusSeconds(900), 900);
        };
    }
}
