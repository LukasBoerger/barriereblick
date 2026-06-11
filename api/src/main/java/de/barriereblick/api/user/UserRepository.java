package de.barriereblick.api.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    /**
     * E-Mail wird vor dem Aufruf im Service auf lowercase normalisiert
     * (Unique-Index auf lower(email) in V1__init.sql).
     */
    Optional<AppUser> findByEmail(String email);
}
