package com.samterminal.backend;

import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.Location;
import com.samterminal.backend.repository.UserLocationUnlockRepository;
import com.samterminal.backend.service.UserLocationUnlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLocationUnlockServiceTest {

    @Mock
    private UserLocationUnlockRepository unlockRepository;

    @Test
    void ensureUnlockedIgnoresDuplicateInsertRollback() {
        UserLocationUnlockService service = new UserLocationUnlockService(
                unlockRepository,
                stubTxManager()
        );
        AppUser user = AppUser.builder().id(1L).username("u").build();
        Location location = Location.builder().id(2L).code("golden-hour").build();

        when(unlockRepository.findFirstByUserAndLocation(user, location))
                .thenReturn(Optional.empty());
        when(unlockRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertDoesNotThrow(() -> service.ensureUnlocked(user, location));
        verify(unlockRepository).saveAndFlush(any());
    }

    @Test
    void ensureUnlockedSkipsExistingUnlock() {
        UserLocationUnlockService service = new UserLocationUnlockService(
                unlockRepository,
                stubTxManager()
        );
        AppUser user = AppUser.builder().id(1L).username("u").build();
        Location location = Location.builder().id(2L).code("golden-hour").build();

        when(unlockRepository.findFirstByUserAndLocation(user, location))
                .thenReturn(Optional.of(new com.samterminal.backend.entity.UserLocationUnlock()));

        assertDoesNotThrow(() -> service.ensureUnlocked(user, location));
        verify(unlockRepository, never()).saveAndFlush(any());
    }

    private static PlatformTransactionManager stubTxManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                if (status.isRollbackOnly()) {
                    throw new UnexpectedRollbackException("rollback");
                }
            }

            @Override
            public void rollback(TransactionStatus status) {
                // no-op
            }
        };
    }
}
