package fr.agroscan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_user_external", columnNames = {"user_id", "external_id"})
)
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false, length = 20)
    private String tone;

    @Column(name = "scan_id")
    private Long scanId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppNotification() {
    }

    public AppNotification(AppUser user, String externalId) {
        this.user = user;
        this.externalId = externalId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getLabel() {
        return label;
    }

    public String getMessage() {
        return message;
    }

    public int getProgress() {
        return progress;
    }

    public String getTone() {
        return tone;
    }

    public Long getScanId() {
        return scanId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String label, String message, int progress, String tone, Long scanId) {
        this.label = label;
        this.message = message;
        this.progress = Math.max(0, Math.min(100, progress));
        this.tone = tone;
        this.scanId = scanId;
        this.updatedAt = Instant.now();
    }
}
