package com.samterminal.backend.repository;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findByUser(AppUser user);
}
