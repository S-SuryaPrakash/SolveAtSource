package com.example.resolveX.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.resolveX.entity.User;
import com.example.resolveX.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRoleAndEnabledTrue(UserRole role);
}
