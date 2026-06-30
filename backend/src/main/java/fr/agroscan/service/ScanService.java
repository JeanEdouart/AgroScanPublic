package fr.agroscan.service;

import fr.agroscan.domain.Scan;
import fr.agroscan.domain.ScanAnalysisStatus;
import fr.agroscan.domain.Plot;
import fr.agroscan.repository.AppUserRepository;
import fr.agroscan.repository.DiseaseSummary;
import fr.agroscan.repository.PlotRepository;
import fr.agroscan.repository.PlotHealthRow;
import fr.agroscan.repository.ScanRepository;
import fr.agroscan.repository.ScanInsightsCounts;
import fr.agroscan.repository.ScanSummary;
import fr.agroscan.repository.ScanSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

@Service
public class ScanService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Instant EARLIEST_UPLOAD = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant LATEST_UPLOAD = Instant.parse("9999-12-31T23:59:59Z");

    private final ScanRepository scanRepository;
    private final AppUserRepository userRepository;
    private final ImageValidationService imageValidationService;
    private final ModelAnalysisClient modelAnalysisClient;
    private final ScanAnalysisPresentationService presentationService;
    private final PlotRepository plotRepository;

    public ScanService(
            ScanRepository scanRepository,
            AppUserRepository userRepository,
            ImageValidationService imageValidationService,
            ModelAnalysisClient modelAnalysisClient,
            ScanAnalysisPresentationService presentationService,
            PlotRepository plotRepository
    ) {
        this.scanRepository = scanRepository;
        this.userRepository = userRepository;
        this.imageValidationService = imageValidationService;
        this.modelAnalysisClient = modelAnalysisClient;
        this.presentationService = presentationService;
        this.plotRepository = plotRepository;
    }

    @Transactional
    public Scan create(
            String email,
            String name,
            String description,
            String imageBase64,
            String thumbnailBase64,
            String imageMediaType,
            String notes,
            String plotName,
            Long plotId,
            Long followUpOfId
    ) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        byte[] imageBytes = imageValidationService
                .validate(imageBase64, imageMediaType, MAX_IMAGE_BYTES, "L'image doit peser moins de 5 Mo")
                .bytes();
        imageValidationService.validateJpeg(
                thumbnailBase64,
                150 * 1024,
                "La miniature est invalide",
                "La miniature est trop volumineuse"
        );
        Scan followUpOf = followUpOfId == null ? null : scanRepository.findByIdAndUserId(followUpOfId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan de suivi introuvable"));
        Plot plot = plotId == null ? null : plotRepository.findByIdAndUserId(plotId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcelle introuvable"));
        Scan scan = new Scan(
                user,
                name.trim(),
                description.trim(),
                imageBase64,
                thumbnailBase64,
                imageMediaType,
                imageBytes.length
        );
        scan.updateWorkflowMetadata(false, false, notes, plotName, plot, followUpOf);
        return scanRepository.save(scan);
    }

    @Transactional(readOnly = true)
    public ScanInsights insights(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        ScanInsightsCounts counts = scanRepository.insightsCounts(
                user.getId(),
                ScanAnalysisStatus.UPLOADED,
                ScanAnalysisStatus.ANALYSIS_PENDING,
                ScanAnalysisStatus.ANALYSIS_RUNNING,
                ScanAnalysisStatus.ANALYSIS_DONE,
                ScanAnalysisStatus.ANALYSIS_FAILED
        );
        List<PlotHealth> plots = scanRepository.plotHealth(user.getId(), 5).stream()
                .map(PlotHealth::from)
                .toList();
        List<DiseaseInsight> diseases = scanRepository.frequentDiseases(user.getId(), PageRequest.of(0, 5)).stream()
                .map(summary -> DiseaseInsight.from(summary, presentationService))
                .toList();
        long attention = counts.diseased() + counts.failed() + counts.pending() + counts.running();
        return new ScanInsights(counts, attention, plots, diseases);
    }

    @Transactional(readOnly = true)
    public Page<ScanSummary> search(
            String email,
            String name,
            Boolean favorite,
            Boolean archived,
            String plotName,
            ScanAnalysisStatus status,
            Boolean healthy,
            String plant,
            String disease,
            Instant uploadedFrom,
            Instant uploadedTo,
            int page,
            int size,
            boolean ascending
    ) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 50));
        Page<ScanSummaryRow> rows = scanRepository.searchRows(
                user.getId(),
                name == null ? "" : name.trim(),
                favorite,
                archived,
                plotName == null ? "" : plotName.trim(),
                status == null ? null : status.name(),
                healthy,
                plant == null ? "" : plant.trim(),
                matchingKeysOrSentinel(presentationService.matchingPlantKeys(plant)),
                disease == null ? "" : disease.trim(),
                matchingKeysOrSentinel(presentationService.matchingDiseaseKeys(disease)),
                uploadedFrom == null ? EARLIEST_UPLOAD : uploadedFrom,
                uploadedTo == null ? LATEST_UPLOAD : uploadedTo,
                ascending,
                pageRequest
        );
        return new PageImpl<>(
                rows.getContent().stream().map(this::toScanSummary).toList(),
                pageRequest,
                rows.getTotalElements()
        );
    }

    @Transactional
    public Scan updateWorkflowMetadata(
            String email,
            Long id,
            Boolean favorite,
            Boolean archived,
            String notes,
            String plotName,
            Long plotId,
            Long followUpOfId
    ) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Scan scan = scanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
        Scan followUpOf = null;
        if (followUpOfId != null) {
            if (followUpOfId.equals(id)) {
                throw new InvalidImageException("Un scan ne peut pas etre son propre suivi");
            }
            followUpOf = scanRepository.findByIdAndUserId(followUpOfId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Scan de suivi introuvable"));
        }
        Plot plot = plotId == null ? null : plotRepository.findByIdAndUserId(plotId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcelle introuvable"));
        scan.updateWorkflowMetadata(favorite, archived, notes, plotName, plot, followUpOf);
        return scan;
    }

    private static java.util.List<String> matchingKeysOrSentinel(java.util.List<String> keys) {
        return keys.isEmpty() ? java.util.List.of("__NO_MATCH__") : keys;
    }

    private ScanSummary toScanSummary(ScanSummaryRow row) {
        return new ScanSummary(
                row.getId(),
                row.getName(),
                row.getDescription(),
                row.getThumbnailBase64(),
                row.getImageMediaType(),
                row.getImageSizeBytes(),
                row.getUploadedAt(),
                row.getFavorite(),
                row.getArchived(),
                row.getPlotId(),
                row.getPlotName(),
                row.getFollowUpOfId(),
                row.getFollowUpName(),
                ScanAnalysisStatus.valueOf(row.getAnalysisStatus()),
                row.getAnalysisPlant(),
                row.getAnalysisDisease(),
                row.getAnalysisHealthy(),
                row.getAnalysisConfidence(),
                row.getAnalyzedAt()
        );
    }

    public record ScanInsights(
            ScanInsightsCounts counts,
            long attention,
            List<PlotHealth> plots,
            List<DiseaseInsight> diseases
    ) {
    }

    public record PlotHealth(
            Long plotId,
            String plotName,
            long total,
            long healthy,
            long diseased,
            Instant latestScanAt
    ) {
        static PlotHealth from(PlotHealthRow summary) {
            return new PlotHealth(
                    summary.getPlotId(),
                    summary.getPlotName(),
                    summary.getTotal(),
                    summary.getHealthy(),
                    summary.getDiseased(),
                    summary.getLatestScanAt()
            );
        }
    }

    public record DiseaseInsight(
            String disease,
            String diseaseLabel,
            long occurrences,
            Instant latestDetectedAt
    ) {
        static DiseaseInsight from(DiseaseSummary summary, ScanAnalysisPresentationService presentationService) {
            return new DiseaseInsight(
                    summary.disease(),
                    presentationService.presentation(null, summary.disease(), false).diseaseLabel(),
                    summary.occurrences(),
                    summary.latestDetectedAt()
            );
        }
    }

    @Transactional
    public void updateAnalysisStatus(String email, Long id, ScanAnalysisStatus status) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Scan scan = scanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
        scan.updateAnalysisStatus(status);
    }

    @Transactional(readOnly = true)
    public Scan get(String email, Long id) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return scanRepository.findDetailByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
    }

    @Transactional
    public ScanAnalysisResult analyze(String email, Long id) {
        return analyze(email, id, (progress, message) -> {
        });
    }

    @Transactional
    public ScanAnalysisResult analyze(String email, Long id, BiConsumer<Integer, String> progress) {
        progress.accept(25, "Recherche du scan");
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Scan scan = scanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
        if (scan.hasAnalysis()) {
            scan.updateAnalysisStatus(ScanAnalysisStatus.ANALYSIS_DONE);
            progress.accept(100, "Analyse deja disponible");
            return ScanAnalysisResult.from(scan);
        }
        scan.updateAnalysisStatus(ScanAnalysisStatus.ANALYSIS_RUNNING);
        progress.accept(45, "Envoi de l'image au modele");
        ScanAnalysisResult result = modelAnalysisClient.analyze(scan.getImageBase64(), scan.getImageMediaType());
        progress.accept(85, "Enregistrement du diagnostic");
        scan.updateAnalysis(
                result.plant(),
                result.disease(),
                result.healthy(),
                result.confidence(),
                result.rawJson()
        );
        progress.accept(95, "Preparation du resultat");
        return ScanAnalysisResult.from(scan);
    }

    @Transactional
    public void delete(String email, Long id) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Scan scan = scanRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan introuvable"));
        scanRepository.delete(scan);
    }

}
