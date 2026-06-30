package fr.agroscan.repository;

import java.time.Instant;
import fr.agroscan.domain.ScanAnalysisStatus;

public record ScanSummary(
        Long id,
        String name,
        String description,
        String thumbnailBase64,
        String imageMediaType,
        long imageSizeBytes,
        Instant uploadedAt,
        boolean favorite,
        boolean archived,
        Long plotId,
        String plotName,
        Long followUpOfId,
        String followUpName,
        ScanAnalysisStatus analysisStatus,
        String analysisPlant,
        String analysisDisease,
        Boolean analysisHealthy,
        Double analysisConfidence,
        Instant analyzedAt
) {
}
