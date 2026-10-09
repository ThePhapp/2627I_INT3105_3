package com.gdrn.disaster.application.port;

import java.util.UUID;

@FunctionalInterface
public interface DisasterIdGenerator {
    UUID next();
}
