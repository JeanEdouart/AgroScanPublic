package fr.agroscan.service;

import fr.agroscan.domain.Scan;

import java.time.Instant;

public record ScanAnalysisResult(
        String plant,
        String disease,
        Boolean healthy,
        Double confidence,
        String rawJson,
        Instant analyzedAt
) {
    public static ScanAnalysisResult from(Scan scan) {
        return new ScanAnalysisResult(
                scan.getAnalysisPlant(),
                scan.getAnalysisDisease(),
                scan.getAnalysisHealthy(),
                scan.getAnalysisConfidence(),
                scan.getAnalysisRawJson(),
                scan.getAnalyzedAt()
        );
    }
}
