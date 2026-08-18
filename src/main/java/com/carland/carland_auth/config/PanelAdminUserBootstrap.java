package com.carland.carland_auth.config;

import com.carland.carland_auth.entity.User;
import com.carland.carland_auth.enums.UserRoles;
import com.carland.carland_auth.enums.UserStatus;
import com.carland.carland_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * tr: Panel admin kullanıcısı (+994000000000 / PIN 2026 / ADMIN) yoksa auth DB'ye ekler.
 * en: Inserts the panel admin user (+994000000000 / PIN 2026 / ADMIN) when missing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PanelAdminUserBootstrap implements ApplicationRunner {

    public static final String PANEL_ADMIN_PHONE = "+994000000000";
    public static final String PANEL_ADMIN_PIN = "2026";

    private final UserRepository userRepository;
    private final Argon2PasswordEncoder argon2PasswordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User existing = userRepository.findByPhoneNumber(PANEL_ADMIN_PHONE);
        if (existing != null) {
            boolean dirty = false;
            if (!UserRoles.ADMIN.name().equals(existing.getRole())) {
                existing.setRole(UserRoles.ADMIN.name());
                dirty = true;
            }
            if (!UserStatus.ACTIVE.name().equalsIgnoreCase(existing.getStatus())) {
                existing.setStatus(UserStatus.ACTIVE.name());
                dirty = true;
            }
            if (existing.getPinHash() == null || existing.getPinHash().isBlank()) {
                existing.setPinHash(argon2PasswordEncoder.encode(PANEL_ADMIN_PIN));
                dirty = true;
            }
            if (dirty) {
                userRepository.save(existing);
                log.info("Panel admin user updated: {}", PANEL_ADMIN_PHONE);
            }
            return;
        }
        userRepository.save(User.builder()
                .phoneNumber(PANEL_ADMIN_PHONE)
                .pinHash(argon2PasswordEncoder.encode(PANEL_ADMIN_PIN))
                .role(UserRoles.ADMIN.name())
                .status(UserStatus.ACTIVE.name())
                .name("Panel")
                .surname("Admin")
                .createdAt(LocalDateTime.now())
                .failedPinAttempts(0)
                .build());
        log.info("Panel admin user created: {} / PIN 2026", PANEL_ADMIN_PHONE);
    }
}
