package fr.agroscan.repository;

import java.time.Instant;

public record PlotHealthSummary(
        Long plotId,
        String plotName,
        long total,
        long healthy,
        long diseased,
        Instant latestScanAt
) {
}
