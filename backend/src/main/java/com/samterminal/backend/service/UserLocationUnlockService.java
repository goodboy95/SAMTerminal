package com.samterminal.backend.service;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.Location;
import com.samterminal.backend.entity.UserLocationUnlock;
import com.samterminal.backend.repository.UserLocationUnlockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Service
public class UserLocationUnlockService {
    private final UserLocationUnlockRepository unlockRepository;
    private final TransactionTemplate transactionTemplate;

    public UserLocationUnlockService(UserLocationUnlockRepository unlockRepository,
                                     PlatformTransactionManager transactionManager) {
        this.unlockRepository = unlockRepository;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate = template;
    }

    public void ensureUnlocked(AppUser user, Location location) {
        if (user == null || location == null) {
            return;
        }
        try {
            transactionTemplate.execute(status -> {
                try {
                    if (unlockRepository.findFirstByUserAndLocation(user, location).isPresent()) {
                        return null;
                    }
                    unlockRepository.saveAndFlush(UserLocationUnlock.builder()
                            .user(user)
                            .location(location)
                            .unlockedAt(Instant.now())
                            .build());
                } catch (DataIntegrityViolationException ex) {
                    status.setRollbackOnly();
                }
                return null;
            });
        } catch (Exception ignored) {
            // Ignore duplicate insert rollbacks or transient transaction failures.
        }
    }
}
