package fr.agroscan.repository;

import java.time.Instant;

public interface ScanSummaryRow {
    Long getId();

    String getName();

    String getDescription();

    String getThumbnailBase64();

    String getImageMediaType();

    long getImageSizeBytes();

    Instant getUploadedAt();

    boolean getFavorite();

    boolean getArchived();

    Long getPlotId();

    String getPlotName();

    Long getFollowUpOfId();

    String getFollowUpName();

    String getAnalysisStatus();

    String getAnalysisPlant();

    String getAnalysisDisease();

    Boolean getAnalysisHealthy();

    Double getAnalysisConfidence();

    Instant getAnalyzedAt();
}
