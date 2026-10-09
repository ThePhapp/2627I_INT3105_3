package com.gdrn.identity.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "identity_credentials")
public class CredentialEntity {
    @Id @Column(name = "user_id") private UUID userId;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    protected CredentialEntity() {}
    public String passwordHash() { return passwordHash; }
}
