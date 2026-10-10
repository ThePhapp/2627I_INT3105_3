package com.gdrn.shared.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorWriter errors) throws Exception {
        var converter = new JwtAuthenticationConverter();
        var bearerTokens = new DefaultBearerTokenResolver();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object role = jwt.getClaims().get("role");
            return "CITIZEN".equals(role) || "AUTHORITY".equals(role)
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.of();
        });
        return http
                // API authentication is explicit Bearer only; no browser-automatic credentials.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/actuator/health",
                                "/actuator/health/liveness", "/actuator/health/readiness",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("CITIZEN", "AUTHORITY")
                        .requestMatchers(HttpMethod.GET, "/api/disasters", "/api/disasters/**")
                        .hasAnyRole("CITIZEN", "AUTHORITY")
                        .requestMatchers(HttpMethod.POST, "/api/disasters").hasRole("AUTHORITY")
                        .requestMatchers(HttpMethod.PATCH, "/api/disasters/**").hasRole("AUTHORITY")
                        .requestMatchers(HttpMethod.POST, "/api/reports").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.PATCH, "/api/reports/*/verification").hasRole("AUTHORITY")
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/*").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.GET, "/api/reports", "/api/reports/*")
                        .hasAnyRole("CITIZEN", "AUTHORITY")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(request -> "POST".equals(request.getMethod()) && "/api/auth/login".equals(request.getServletPath())
                                ? null : bearerTokens.resolve(request))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, ex) -> errors.write(request, response, 401))
                        .accessDeniedHandler((request, response, ex) -> errors.write(request, response, 403)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, error) -> errors.write(request, response, 401))
                        .accessDeniedHandler((request, response, error) -> errors.write(request, response, 403)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .build();
    }
}
