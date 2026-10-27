package com.example.resolveX.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time administrator bootstrap values supplied through deployment configuration.
 *
 * <p>The values are intentionally disabled by default and are not user account
 * data persisted by this class. A later initializer will validate and use them
 * only when bootstrap is explicitly enabled.</p>
 */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String name,
        String email,
        String password
) {
}
