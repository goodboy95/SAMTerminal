package com.samterminal.backend.service;

import com.samterminal.backend.config.AppProperties;
import com.samterminal.backend.entity.AppUser;
import com.samterminal.backend.entity.UserRole;
import com.samterminal.backend.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AdminAccountService {
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AppProperties appProperties;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(AppProperties appProperties,
                               AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void syncAdmins() {
        if (appProperties.getAdmins() == null || appProperties.getAdmins().isEmpty()) {
            return;
        }
        for (AppProperties.AdminAccount admin : appProperties.getAdmins()) {
            if (!StringUtils.hasText(admin.getUsername())) {
                continue;
            }
            if (!StringUtils.hasText(admin.getPassword()) || admin.getPassword().length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("Admin password must be at least 12 characters.");
            }
            AppUser user = userRepository.findByUsername(admin.getUsername())
                    .orElseGet(() -> AppUser.builder().username(admin.getUsername()).build());
            user.setEmail(admin.getEmail());
            user.setRole(UserRole.ADMIN);
            if (user.getPassword() == null || !passwordEncoder.matches(admin.getPassword(), user.getPassword())) {
                user.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
            userRepository.save(user);
        }
    }
}
