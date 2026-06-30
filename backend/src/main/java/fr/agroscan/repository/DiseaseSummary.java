package fr.agroscan.repository;

import java.time.Instant;

public record DiseaseSummary(
        String disease,
        long occurrences,
        Instant latestDetectedAt
) {
}
