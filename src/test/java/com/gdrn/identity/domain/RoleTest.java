package com.gdrn.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void containsTheInitialIdentityRoleVocabulary() {
        assertEquals(
                EnumSet.of(Role.CITIZEN, Role.RESPONDER, Role.AUTHORITY, Role.ADMIN),
                EnumSet.allOf(Role.class));
    }
}
