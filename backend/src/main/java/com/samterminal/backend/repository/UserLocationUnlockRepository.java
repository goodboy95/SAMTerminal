package com.samterminal.backend.repository;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.Location;
import com.samterminal.backend.entity.UserLocationUnlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLocationUnlockRepository extends JpaRepository<UserLocationUnlock, Long> {
    List<UserLocationUnlock> findByUser(AppUser user);
    Optional<UserLocationUnlock> findFirstByUserAndLocation(AppUser user, Location location);
}
