package fr.agroscan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "scans")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "image_base64", nullable = false, columnDefinition = "TEXT")
    private String imageBase64;

    @Column(name = "thumbnail_base64", nullable = false, columnDefinition = "TEXT")
    private String thumbnailBase64;

    @Column(name = "image_media_type", nullable = false, length = 40)
    private String imageMediaType;

    @Column(name = "image_size_bytes", nullable = false)
    private long imageSizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private boolean favorite;

    @Column(nullable = false)
    private boolean archived;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Column(name = "plot_name", length = 160)
    private String plotName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id")
    private Plot plot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_of_id")
    private Scan followUpOf;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    private ScanAnalysisStatus analysisStatus;

    @Column(name = "analysis_plant", length = 120)
    private String analysisPlant;

    @Column(name = "analysis_disease", length = 200)
    private String analysisDisease;

    @Column(name = "analysis_healthy")
    private Boolean analysisHealthy;

    @Column(name = "analysis_confidence")
    private Double analysisConfidence;

    @Column(name = "analysis_raw_json", columnDefinition = "TEXT")
    private String analysisRawJson;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    protected Scan() {
    }

    public Scan(AppUser user, String name, String description, String imageBase64, String thumbnailBase64, String imageMediaType, long imageSizeBytes) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.imageBase64 = imageBase64;
        this.thumbnailBase64 = thumbnailBase64;
        this.imageMediaType = imageMediaType;
        this.imageSizeBytes = imageSizeBytes;
        this.uploadedAt = Instant.now();
        this.analysisStatus = ScanAnalysisStatus.UPLOADED;
        this.notes = "";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public String getImageMediaType() {
        return imageMediaType;
    }

    public String getThumbnailBase64() {
        return thumbnailBase64;
    }

    public long getImageSizeBytes() {
        return imageSizeBytes;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public ScanAnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public boolean isArchived() {
        return archived;
    }

    public String getNotes() {
        return notes;
    }

    public String getPlotName() {
        return plot == null ? plotName : plot.getName();
    }

    public Plot getPlot() {
        return plot;
    }

    public Scan getFollowUpOf() {
        return followUpOf;
    }

    public String getAnalysisPlant() {
        return analysisPlant;
    }

    public String getAnalysisDisease() {
        return analysisDisease;
    }

    public Boolean getAnalysisHealthy() {
        return analysisHealthy;
    }

    public Double getAnalysisConfidence() {
        return analysisConfidence;
    }

    public String getAnalysisRawJson() {
        return analysisRawJson;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public boolean hasAnalysis() {
        return analyzedAt != null;
    }

    public void updateAnalysisStatus(ScanAnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public void updateWorkflowMetadata(
            Boolean favorite,
            Boolean archived,
            String notes,
            String plotName,
            Plot plot,
            Scan followUpOf
    ) {
        if (favorite != null) this.favorite = favorite;
        if (archived != null) this.archived = archived;
        if (notes != null) this.notes = notes.trim();
        if (plot != null || plotName != null) {
            this.plot = plot;
            this.plotName = plot == null && !plotName.isBlank() ? plotName.trim() : null;
        }
        this.followUpOf = followUpOf;
    }

    public void updateAnalysis(
            String plant,
            String disease,
            Boolean healthy,
            Double confidence,
            String rawJson
    ) {
        this.analysisPlant = plant;
        this.analysisDisease = disease;
        this.analysisHealthy = healthy;
        this.analysisConfidence = confidence;
        this.analysisRawJson = rawJson;
        this.analyzedAt = Instant.now();
        this.analysisStatus = ScanAnalysisStatus.ANALYSIS_DONE;
    }
}
