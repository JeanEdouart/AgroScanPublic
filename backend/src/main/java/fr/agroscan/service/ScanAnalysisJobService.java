package fr.agroscan.service;

import fr.agroscan.domain.ScanAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanAnalysisJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanAnalysisJobService.class);

    private final Map<UUID, ScanAnalysisJob> jobs = new ConcurrentHashMap<>();
    private final ScanAnalysisJobWorker worker;
    private final AppNotificationService notificationService;
    private final ScanService scanService;

    public ScanAnalysisJobService(
            ScanAnalysisJobWorker worker,
            AppNotificationService notificationService,
            ScanService scanService
    ) {
        this.worker = worker;
        this.notificationService = notificationService;
        this.scanService = scanService;
    }

    public ScanAnalysisJobSnapshot start(String email, Long scanId, String notificationId, String scanName) {
        UUID jobId = UUID.randomUUID();
        ScanAnalysisJob job = new ScanAnalysisJob(jobId, email, scanId, notificationId, scanName);
        jobs.put(jobId, job);
        scanService.updateAnalysisStatus(email, scanId, ScanAnalysisStatus.ANALYSIS_PENDING);
        notifySafely(email, notificationId, scanName, scanId);
        worker.run(job);
        return job.snapshot();
    }

    private void notifySafely(String email, String notificationId, String scanName, Long scanId) {
        try {
            notificationService.upsert(email, notificationId, "Analyse du scan", scanName + " - Analyse en attente", 5, "active", scanId);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to publish queued analysis notification {}", notificationId, exception);
        }
    }

    public ScanAnalysisJobSnapshot get(String email, UUID jobId) {
        ScanAnalysisJob job = jobs.get(jobId);
        if (job == null || !job.email().equalsIgnoreCase(email)) {
            throw new ResourceNotFoundException("Analyse introuvable");
        }
        return job.snapshot();
    }

    static class ScanAnalysisJob {
        private final UUID id;
        private final String email;
        private final Long scanId;
        private final String notificationId;
        private final String scanName;
        private final Instant createdAt;
        private volatile ScanAnalysisJobStatus status;
        private volatile int progress;
        private volatile String message;
        private volatile ScanAnalysisResult result;
        private volatile String error;
        private volatile Instant updatedAt;

        ScanAnalysisJob(UUID id, String email, Long scanId, String notificationId, String scanName) {
            this.id = id;
            this.email = email;
            this.scanId = scanId;
            this.notificationId = notificationId;
            this.scanName = scanName;
            this.createdAt = Instant.now();
            this.updatedAt = createdAt;
            update(ScanAnalysisJobStatus.QUEUED, 5, "Analyse en attente");
        }

        UUID id() {
            return id;
        }

        String email() {
            return email;
        }

        Long scanId() {
            return scanId;
        }

        String notificationId() {
            return notificationId;
        }

        String scanName() {
            return scanName;
        }

        void update(ScanAnalysisJobStatus status, int progress, String message) {
            this.status = status;
            this.progress = progress;
            this.message = message;
            this.updatedAt = Instant.now();
        }

        void complete(ScanAnalysisResult result) {
            this.result = result;
            update(ScanAnalysisJobStatus.COMPLETED, 100, "Analyse terminee");
        }

        void fail(String message) {
            this.error = message;
            update(ScanAnalysisJobStatus.FAILED, 100, "Analyse interrompue");
        }

        ScanAnalysisJobSnapshot snapshot() {
            return new ScanAnalysisJobSnapshot(
                    id,
                    scanId,
                    status,
                    progress,
                    message,
                    result,
                    error,
                    createdAt,
                    updatedAt
            );
        }
    }

    public enum ScanAnalysisJobStatus {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public record ScanAnalysisJobSnapshot(
            UUID id,
            Long scanId,
            ScanAnalysisJobStatus status,
            int progress,
            String message,
            ScanAnalysisResult result,
            String error,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
