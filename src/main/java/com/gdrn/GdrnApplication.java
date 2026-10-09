package com.gdrn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Identity provisions its own accounts; never create a generated development user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class GdrnApplication {
    public static void main(String[] args) {
        SpringApplication.run(GdrnApplication.class, args);
    }
}
