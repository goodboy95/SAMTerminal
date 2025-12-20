package com.samterminal.backend.repository;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.UserTokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenUsageRepository extends JpaRepository<UserTokenUsage, Long> {
    Optional<UserTokenUsage> findByUser(AppUser user);
}
