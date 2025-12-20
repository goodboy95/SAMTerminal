package com.samterminal.backend.repository;

import com.samterminal.backend.entity.NpcCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NpcCharacterRepository extends JpaRepository<NpcCharacter, Long> {
}
