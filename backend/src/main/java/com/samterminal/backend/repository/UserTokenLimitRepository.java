package com.samterminal.backend.repository;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.UserTokenLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenLimitRepository extends JpaRepository<UserTokenLimit, Long> {
    Optional<UserTokenLimit> findByUser(AppUser user);
}
