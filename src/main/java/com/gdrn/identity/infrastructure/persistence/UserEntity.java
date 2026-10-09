package com.gdrn.identity.infrastructure.persistence;

import com.gdrn.identity.domain.EmailAddress;
import com.gdrn.identity.domain.Role;
import com.gdrn.identity.domain.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "identity_users")
public class UserEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private Role role;
    protected UserEntity() {}
    public User toDomain() { return User.reconstitute(id, EmailAddress.of(email), role); }
}
