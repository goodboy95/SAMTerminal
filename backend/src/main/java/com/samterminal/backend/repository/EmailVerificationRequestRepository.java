package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailVerificationRequest;
import com.samterminal.backend.entity.EmailVerificationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRequestRepository extends JpaRepository<EmailVerificationRequest, String> {
    Optional<EmailVerificationRequest> findTopByEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("update EmailVerificationRequest r set r.status = :status where r.email = :email and r.status in :activeStatuses")
    int markByEmailWithStatus(@Param("email") String email,
                              @Param("status") EmailVerificationRequestStatus status,
                              @Param("activeStatuses") Iterable<EmailVerificationRequestStatus> activeStatuses);

    @Modifying
    @Query("update EmailVerificationRequest r set r.status = :status where r.expiresAt < :now and r.status = :pending")
    int expirePending(@Param("status") EmailVerificationRequestStatus status,
                      @Param("pending") EmailVerificationRequestStatus pending,
                      @Param("now") LocalDateTime now);
}
