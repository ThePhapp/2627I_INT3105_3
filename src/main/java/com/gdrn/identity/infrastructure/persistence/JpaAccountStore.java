package com.gdrn.identity.infrastructure.persistence;

import com.gdrn.identity.application.port.AccountStore;
import com.gdrn.identity.domain.EmailAddress;
import com.gdrn.identity.domain.User;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaAccountStore implements AccountStore {
    private final EntityManager entityManager;
    public JpaAccountStore(EntityManager entityManager) { this.entityManager = entityManager; }

    public Optional<Account> findByEmail(EmailAddress email) {
        return entityManager.createQuery("select u from UserEntity u where u.email = :email", UserEntity.class)
                .setParameter("email", email.value()).getResultList().stream().findFirst().map(entity -> {
                    var user = entity.toDomain();
                    var credential = entityManager.find(CredentialEntity.class, user.id());
                    if (credential == null) throw new IllegalStateException("Account credential missing");
                    return new Account(user, credential.passwordHash());
                });
    }

    public Optional<User> findUser(UUID id) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, id)).map(UserEntity::toDomain);
    }

    @Transactional
    public void createIfAbsent(User user, String passwordHash) {
        // Atomic first-writer-wins makes repeated/concurrent demo starts non-destructive.
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO identity_users (id, email, role) VALUES (:id, :email, :role)
                ON CONFLICT (email) DO NOTHING
                """).setParameter("id", user.id()).setParameter("email", user.emailAddress().value())
                .setParameter("role", user.role().name()).executeUpdate();
        if (inserted == 1) {
            entityManager.createNativeQuery("INSERT INTO identity_credentials (user_id, password_hash) VALUES (:id, :hash)")
                    .setParameter("id", user.id()).setParameter("hash", passwordHash).executeUpdate();
        }
    }
}
