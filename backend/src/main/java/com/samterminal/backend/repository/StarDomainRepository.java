package com.samterminal.backend.repository;

import com.samterminal.backend.entity.StarDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StarDomainRepository extends JpaRepository<StarDomain, Long> {
    Optional<StarDomain> findByCode(String code);
}
