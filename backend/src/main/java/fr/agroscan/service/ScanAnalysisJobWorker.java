package fr.agroscan.service;

import fr.agroscan.domain.ScanAnalysisStatus;
import fr.agroscan.service.ScanAnalysisJobService.ScanAnalysisJob;
import fr.agroscan.service.ScanAnalysisJobService.ScanAnalysisJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ScanAnalysisJobWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanAnalysisJobWorker.class);

    private final ScanService scanService;
    private final AppNotificationService notificationService;

    public ScanAnalysisJobWorker(ScanService scanService, AppNotificationService notificationService) {
        this.scanService = scanService;
        this.notificationService = notificationService;
    }

    @Async("analysisTaskExecutor")
    public void run(ScanAnalysisJob job) {
        try {
            job.update(ScanAnalysisJobStatus.RUNNING, 20, "Preparation de l'image");
            notifySafely(job, "active");
            ScanAnalysisResult result = scanService.analyze(job.email(), job.scanId(), (progress, message) ->
            {
                job.update(ScanAnalysisJobStatus.RUNNING, progress, message);
                notifySafely(job, "active");
            }
            );
            job.complete(result);
            notifySafely(job, "success");
        } catch (RuntimeException exception) {
            scanService.updateAnalysisStatus(job.email(), job.scanId(), ScanAnalysisStatus.ANALYSIS_FAILED);
            job.fail(exception.getMessage() == null ? "L'analyse a echoue" : exception.getMessage());
            notifySafely(job, "error");
        }
    }

    private void notifySafely(ScanAnalysisJob job, String tone) {
        try {
            notificationService.upsert(
                    job.email(),
                    job.notificationId(),
                    "Analyse du scan",
                    job.scanName() + " - " + job.snapshot().message(),
                    job.snapshot().progress(),
                    tone,
                    job.scanId()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to publish analysis notification {}", job.notificationId(), exception);
        }
    }
}
