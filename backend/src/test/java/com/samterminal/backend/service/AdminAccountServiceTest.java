package com.samterminal.backend.service;

import com.samterminal.backend.config.AppProperties;
import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.UserRole;
import com.samterminal.backend.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AppProperties appProperties;
    private AdminAccountService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        service = new AdminAccountService(appProperties, userRepository, passwordEncoder);
    }

    @Test
    void syncAdmins_createsAndUpdatesAdminsFromConfig() {
        AppProperties.AdminAccount admin1 = new AppProperties.AdminAccount();
        admin1.setUsername("admin");
        admin1.setPassword("AdminPass123456");
        admin1.setEmail("admin@sam.local");

        AppProperties.AdminAccount admin2 = new AppProperties.AdminAccount();
        admin2.setUsername("operator");
        admin2.setPassword("OperatorPass123456");
        admin2.setEmail("operator@sam.local");

        appProperties.setAdmins(List.of(admin1, admin2));

        AppUser existing = AppUser.builder()
                .username("operator")
                .password("encoded-old")
                .role(UserRole.USER)
                .email("old@sam.local")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(existing));

        when(passwordEncoder.encode(any())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");
        when(passwordEncoder.matches(eq("OperatorPass123456"), eq("encoded-old"))).thenReturn(false);

        service.syncAdmins();

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository, times(2)).save(saved.capture());

        List<AppUser> users = saved.getAllValues();
        assertThat(users).hasSize(2);

        AppUser u1 = users.getFirst();
        assertThat(u1.getUsername()).isEqualTo("admin");
        assertThat(u1.getEmail()).isEqualTo("admin@sam.local");
        assertThat(u1.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(u1.getPassword()).isEqualTo("ENC(AdminPass123456)");

        AppUser u2 = users.getLast();
        assertThat(u2.getUsername()).isEqualTo("operator");
        assertThat(u2.getEmail()).isEqualTo("operator@sam.local");
        assertThat(u2.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(u2.getPassword()).isEqualTo("ENC(OperatorPass123456)");
    }

    @Test
    void syncAdmins_noConfig_noOps() {
        appProperties.setAdmins(List.of());
        service.syncAdmins();
        verifyNoInteractions(userRepository, passwordEncoder);
    }
}
