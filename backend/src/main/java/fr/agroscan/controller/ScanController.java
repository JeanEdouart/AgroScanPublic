package fr.agroscan.controller;

import fr.agroscan.domain.Scan;
import fr.agroscan.domain.ScanAnalysisStatus;
import fr.agroscan.repository.ScanSummary;
import fr.agroscan.service.ScanAnalysisResult;
import fr.agroscan.service.ScanAnalysisPresentationService;
import fr.agroscan.service.ScanAnalysisPresentationService.AnalysisPresentation;
import fr.agroscan.service.ScanAnalysisJobService;
import fr.agroscan.service.ScanAnalysisJobService.ScanAnalysisJobSnapshot;
import fr.agroscan.service.ScanService;
import fr.agroscan.service.ScanService.DiseaseInsight;
import fr.agroscan.service.ScanService.PlotHealth;
import fr.agroscan.service.ScanService.ScanInsights;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scans")
public class ScanController {

    private final ScanService scanService;
    private final ScanAnalysisPresentationService presentationService;
    private final ScanAnalysisJobService analysisJobService;

    public ScanController(
            ScanService scanService,
            ScanAnalysisPresentationService presentationService,
            ScanAnalysisJobService analysisJobService
    ) {
        this.scanService = scanService;
        this.presentationService = presentationService;
        this.analysisJobService = analysisJobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreatedScanResponse create(Authentication authentication, @Valid @RequestBody CreateScanRequest request) {
        return CreatedScanResponse.from(scanService.create(
                authentication.getName(),
                request.name(),
                request.description(),
                request.imageBase64(),
                request.thumbnailBase64(),
                request.imageMediaType(),
                request.notes(),
                request.plotName(),
                request.plotId(),
                request.followUpOfId()
        ));
    }

    @GetMapping
    PageResponse<ScanSummaryResponse> search(
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String plotName,
            @RequestParam(required = false) ScanAnalysisStatus status,
            @RequestParam(required = false) Boolean healthy,
            @RequestParam(required = false) String plant,
            @RequestParam(required = false) String disease,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant uploadedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant uploadedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "false") boolean ascending
    ) {
        Page<ScanSummary> results = scanService.search(
                authentication.getName(), name, favorite, archived, plotName,
                status, healthy, plant, disease, uploadedFrom, uploadedTo, page, size, ascending
        );
        return PageResponse.from(results.map(scan -> ScanSummaryResponse.from(scan, presentationService)));
    }

    @GetMapping("/insights")
    ScanInsightsResponse insights(Authentication authentication) {
        return ScanInsightsResponse.from(scanService.insights(authentication.getName()));
    }

    @GetMapping("/{id}")
    ScanDetailResponse detail(Authentication authentication, @PathVariable Long id) {
        return ScanDetailResponse.from(scanService.get(authentication.getName(), id), presentationService);
    }

    @PatchMapping("/{id}/workflow")
    ScanDetailResponse updateWorkflow(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateScanWorkflowRequest request
    ) {
        return ScanDetailResponse.from(scanService.updateWorkflowMetadata(
                authentication.getName(),
                id,
                request.favorite(),
                request.archived(),
                request.notes(),
                request.plotName(),
                request.plotId(),
                request.followUpOfId()
        ), presentationService);
    }

    @PostMapping("/{id}/analysis")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ScanAnalysisJobResponse analyze(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) StartAnalysisRequest request
    ) {
        String notificationId = request == null || request.notificationId() == null || request.notificationId().isBlank()
                ? "analysis-" + id + "-" + UUID.randomUUID()
                : request.notificationId();
        String scanName = request == null || request.scanName() == null || request.scanName().isBlank()
                ? "Scan " + id
                : request.scanName();
        return ScanAnalysisJobResponse.from(
                analysisJobService.start(authentication.getName(), id, notificationId, scanName),
                presentationService
        );
    }

    @GetMapping("/analysis-jobs/{jobId}")
    ScanAnalysisJobResponse analysisJob(Authentication authentication, @PathVariable UUID jobId) {
        return ScanAnalysisJobResponse.from(
                analysisJobService.get(authentication.getName(), jobId),
                presentationService
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication authentication, @PathVariable Long id) {
        scanService.delete(authentication.getName(), id);
    }

    record CreateScanRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 7_000_000) String imageBase64,
            @NotBlank @Size(max = 250_000) String thumbnailBase64,
            @NotBlank @Size(max = 40) String imageMediaType,
            @Size(max = 4000) String notes,
            @Size(max = 160) String plotName,
            Long plotId,
            Long followUpOfId
    ) {
    }

    record UpdateScanWorkflowRequest(
            Boolean favorite,
            Boolean archived,
            @Size(max = 4000) String notes,
            @Size(max = 160) String plotName,
            Long plotId,
            Long followUpOfId
    ) {
    }

    record StartAnalysisRequest(
            @Size(max = 120) String notificationId,
            @Size(max = 120) String scanName
    ) {
    }

    record ScanInsightsResponse(
            long total,
            long uploaded,
            long pending,
            long running,
            long analyzed,
            long failed,
            long healthy,
            long diseased,
            long attention,
            List<PlotHealthResponse> plots,
            List<DiseaseInsightResponse> diseases
    ) {
        static ScanInsightsResponse from(ScanInsights insights) {
            return new ScanInsightsResponse(
                    insights.counts().total(),
                    insights.counts().uploaded(),
                    insights.counts().pending(),
                    insights.counts().running(),
                    insights.counts().analyzed(),
                    insights.counts().failed(),
                    insights.counts().healthy(),
                    insights.counts().diseased(),
                    insights.attention(),
                    insights.plots().stream().map(PlotHealthResponse::from).toList(),
                    insights.diseases().stream().map(DiseaseInsightResponse::from).toList()
            );
        }
    }

    record PlotHealthResponse(
            Long plotId,
            String plotName,
            long total,
            long healthy,
            long diseased,
            Instant latestScanAt
    ) {
        static PlotHealthResponse from(PlotHealth plot) {
            return new PlotHealthResponse(
                    plot.plotId(),
                    plot.plotName(),
                    plot.total(),
                    plot.healthy(),
                    plot.diseased(),
                    plot.latestScanAt()
            );
        }
    }

    record DiseaseInsightResponse(
            String disease,
            String diseaseLabel,
            long occurrences,
            Instant latestDetectedAt
    ) {
        static DiseaseInsightResponse from(DiseaseInsight disease) {
            return new DiseaseInsightResponse(
                    disease.disease(),
                    disease.diseaseLabel(),
                    disease.occurrences(),
                    disease.latestDetectedAt()
            );
        }
    }

    record ScanSummaryResponse(
            Long id,
            String name,
            String description,
            String thumbnailDataUrl,
            String imageMediaType,
            long imageSizeBytes,
            Instant uploadedAt,
            boolean favorite,
            boolean archived,
            Long plotId,
            String plotName,
            Long followUpOfId,
            String followUpName,
            String analysisStatus,
            ScanAnalysisResponse analysis
    ) {
        static ScanSummaryResponse from(ScanSummary scan, ScanAnalysisPresentationService presentationService) {
            return new ScanSummaryResponse(
                    scan.id(), scan.name(), scan.description(),
                    "data:image/jpeg;base64," + scan.thumbnailBase64(),
                    scan.imageMediaType(), scan.imageSizeBytes(), scan.uploadedAt(),
                    scan.favorite(), scan.archived(), scan.plotId(), scan.plotName(), scan.followUpOfId(), scan.followUpName(),
                    scan.analysisStatus().name(),
                    ScanAnalysisResponse.from(scan, presentationService)
            );
        }
    }

    record CreatedScanResponse(
            Long id,
            String name,
            String description,
            String imageMediaType,
            long imageSizeBytes,
            Instant uploadedAt
    ) {
        static CreatedScanResponse from(Scan scan) {
            return new CreatedScanResponse(
                    scan.getId(), scan.getName(), scan.getDescription(),
                    scan.getImageMediaType(), scan.getImageSizeBytes(), scan.getUploadedAt()
            );
        }
    }

    record ScanDetailResponse(
            Long id,
            String name,
            String description,
            String imageDataUrl,
            String imageMediaType,
            long imageSizeBytes,
            Instant uploadedAt,
            boolean favorite,
            boolean archived,
            String notes,
            Long plotId,
            String plotName,
            Long followUpOfId,
            String followUpName,
            String analysisStatus,
            ScanAnalysisResponse analysis
    ) {
        static ScanDetailResponse from(Scan scan, ScanAnalysisPresentationService presentationService) {
            return new ScanDetailResponse(
                    scan.getId(), scan.getName(), scan.getDescription(),
                    "data:" + scan.getImageMediaType() + ";base64," + scan.getImageBase64(),
                    scan.getImageMediaType(), scan.getImageSizeBytes(), scan.getUploadedAt(),
                    scan.isFavorite(), scan.isArchived(), scan.getNotes(),
                    scan.getPlot() == null ? null : scan.getPlot().getId(),
                    scan.getPlotName(),
                    scan.getFollowUpOf() == null ? null : scan.getFollowUpOf().getId(),
                    scan.getFollowUpOf() == null ? null : scan.getFollowUpOf().getName(),
                    scan.getAnalysisStatus().name(),
                    ScanAnalysisResponse.from(scan, presentationService)
            );
        }
    }

    record ScanAnalysisResponse(
            String plant,
            String plantLabel,
            String disease,
            String diseaseLabel,
            Boolean healthy,
            Double confidence,
            List<String> advice,
            String rawJson,
            Instant analyzedAt
    ) {
        static ScanAnalysisResponse from(Scan scan, ScanAnalysisPresentationService presentationService) {
            if (!scan.hasAnalysis()) return null;
            AnalysisPresentation presentation = presentationService.presentation(
                    scan.getAnalysisPlant(), scan.getAnalysisDisease(), scan.getAnalysisHealthy()
            );
            return new ScanAnalysisResponse(
                    scan.getAnalysisPlant(),
                    presentation.plantLabel(),
                    scan.getAnalysisDisease(),
                    presentation.diseaseLabel(),
                    scan.getAnalysisHealthy(),
                    scan.getAnalysisConfidence(),
                    presentation.advice(),
                    scan.getAnalysisRawJson(),
                    scan.getAnalyzedAt()
            );
        }

        static ScanAnalysisResponse from(ScanSummary scan, ScanAnalysisPresentationService presentationService) {
            if (scan.analyzedAt() == null) return null;
            AnalysisPresentation presentation = presentationService.presentation(
                    scan.analysisPlant(), scan.analysisDisease(), scan.analysisHealthy()
            );
            return new ScanAnalysisResponse(
                    scan.analysisPlant(),
                    presentation.plantLabel(),
                    scan.analysisDisease(),
                    presentation.diseaseLabel(),
                    scan.analysisHealthy(),
                    scan.analysisConfidence(),
                    presentation.advice(),
                    null,
                    scan.analyzedAt()
            );
        }

        static ScanAnalysisResponse from(ScanAnalysisResult result, ScanAnalysisPresentationService presentationService) {
            AnalysisPresentation presentation = presentationService.presentation(
                    result.plant(), result.disease(), result.healthy()
            );
            return new ScanAnalysisResponse(
                    result.plant(),
                    presentation.plantLabel(),
                    result.disease(),
                    presentation.diseaseLabel(),
                    result.healthy(),
                    result.confidence(),
                    presentation.advice(),
                    result.rawJson(),
                    result.analyzedAt()
            );
        }
    }

    record ScanAnalysisJobResponse(
            UUID id,
            Long scanId,
            String status,
            int progress,
            String message,
            String error,
            Instant createdAt,
            Instant updatedAt,
            ScanAnalysisResponse analysis
    ) {
        static ScanAnalysisJobResponse from(
                ScanAnalysisJobSnapshot job,
                ScanAnalysisPresentationService presentationService
        ) {
            return new ScanAnalysisJobResponse(
                    job.id(),
                    job.scanId(),
                    job.status().name(),
                    job.progress(),
                    job.message(),
                    job.error(),
                    job.createdAt(),
                    job.updatedAt(),
                    job.result() == null ? null : ScanAnalysisResponse.from(job.result(), presentationService)
            );
        }
    }

    record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
            );
        }
    }
}
