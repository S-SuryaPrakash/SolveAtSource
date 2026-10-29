package com.example.resolveX.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.resolveX.entity.User;
import com.example.resolveX.entity.UserRole;
import com.example.resolveX.repository.UserRepository;

/** Creates the initial ADMIN account only when explicitly enabled by deployment configuration. */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private static final String HISTORICAL_IMPORT_EMAIL = "system@resolvex.local";

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminInitializer(BootstrapAdminProperties properties, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        if (userRepository.existsByRoleAndEnabledTrue(UserRole.ADMIN)) {
            log.info("An enabled ADMIN account already exists; bootstrap provisioning is skipped");
            return;
        }

        String name = requiredValue(properties.name(), "BOOTSTRAP_ADMIN_NAME");
        String email = normalizeEmail(requiredValue(properties.email(), "BOOTSTRAP_ADMIN_EMAIL"));
        String password = requiredPassword(properties.password());
        validate(name, email, password);

        if (HISTORICAL_IMPORT_EMAIL.equals(email)) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL is reserved for the Historical Import account");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Bootstrap ADMIN email is already in use");
        }

        User admin = new User(name, email, passwordEncoder.encode(password), UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Created the initial ADMIN account for {}", email);
    }

    private String requiredValue(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be set when bootstrap provisioning is enabled");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private String requiredPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be set when bootstrap provisioning is enabled");
        }
        return password;
    }

    private void validate(String name, String email, String password) {
        if (name.length() > 150) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_NAME must be at most 150 characters");
        }
        if (email.length() > 320 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL must be a valid email address");
        }
        if (password.length() < 15 || password.length() > 128) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be between 15 and 128 characters");
        }
    }
}
