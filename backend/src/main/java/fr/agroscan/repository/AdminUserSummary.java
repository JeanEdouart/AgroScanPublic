package fr.agroscan.repository;

import java.time.Instant;

public record AdminUserSummary(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
