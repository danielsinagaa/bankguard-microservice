package com.bankguard.transaction.repository;

import com.bankguard.transaction.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByUsernameAndActiveTrue(String username);
}
