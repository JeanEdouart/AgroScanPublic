package fr.agroscan.repository;

import fr.agroscan.domain.Scan;
import fr.agroscan.domain.ScanAnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    @Query(value = """
            SELECT
                s.id AS id,
                s.name AS name,
                s.description AS description,
                s.thumbnail_base64 AS thumbnailBase64,
                s.image_media_type AS imageMediaType,
                s.image_size_bytes AS imageSizeBytes,
                s.uploaded_at AS uploadedAt,
                s.favorite AS favorite,
                s.archived AS archived,
                p.id AS plotId,
                COALESCE(p.name, s.plot_name) AS plotName,
                f.id AS followUpOfId,
                f.name AS followUpName,
                s.analysis_status AS analysisStatus,
                s.analysis_plant AS analysisPlant,
                s.analysis_disease AS analysisDisease,
                s.analysis_healthy AS analysisHealthy,
                s.analysis_confidence AS analysisConfidence,
                s.analyzed_at AS analyzedAt
            FROM scans s
            LEFT JOIN plots p ON p.id = s.plot_id
            LEFT JOIN scans f ON f.id = s.follow_up_of_id
            WHERE s.user_id = :userId
              AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))
              AND (:favorite IS NULL OR s.favorite = :favorite)
              AND (:archived IS NULL OR s.archived = :archived)
              AND LOWER(COALESCE(p.name, s.plot_name, '')) LIKE LOWER(CONCAT('%', :plotName, '%'))
              AND (:status IS NULL OR s.analysis_status = :status)
              AND (:healthy IS NULL OR s.analysis_healthy = :healthy)
              AND (
                :plant = ''
                OR s.analysis_plant IN (:plantKeys)
                OR LOWER(COALESCE(s.analysis_plant, '')) LIKE LOWER(CONCAT('%', :plant, '%'))
              )
              AND (
                :disease = ''
                OR s.analysis_disease IN (:diseaseKeys)
                OR LOWER(COALESCE(s.analysis_disease, '')) LIKE LOWER(CONCAT('%', :disease, '%'))
              )
              AND s.uploaded_at >= :uploadedFrom
              AND s.uploaded_at < :uploadedTo
            ORDER BY
              CASE WHEN :ascending = TRUE THEN s.uploaded_at END ASC,
              CASE WHEN :ascending = FALSE THEN s.uploaded_at END DESC
            """,
            countQuery = """
            SELECT COUNT(s.id)
            FROM scans s
            LEFT JOIN plots p ON p.id = s.plot_id
            WHERE s.user_id = :userId
              AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))
              AND (:favorite IS NULL OR s.favorite = :favorite)
              AND (:archived IS NULL OR s.archived = :archived)
              AND LOWER(COALESCE(p.name, s.plot_name, '')) LIKE LOWER(CONCAT('%', :plotName, '%'))
              AND (:status IS NULL OR s.analysis_status = :status)
              AND (:healthy IS NULL OR s.analysis_healthy = :healthy)
              AND (
                :plant = ''
                OR s.analysis_plant IN (:plantKeys)
                OR LOWER(COALESCE(s.analysis_plant, '')) LIKE LOWER(CONCAT('%', :plant, '%'))
              )
              AND (
                :disease = ''
                OR s.analysis_disease IN (:diseaseKeys)
                OR LOWER(COALESCE(s.analysis_disease, '')) LIKE LOWER(CONCAT('%', :disease, '%'))
              )
              AND s.uploaded_at >= :uploadedFrom
              AND s.uploaded_at < :uploadedTo
            """,
            nativeQuery = true)
    Page<ScanSummaryRow> searchRows(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("favorite") Boolean favorite,
            @Param("archived") Boolean archived,
            @Param("plotName") String plotName,
            @Param("status") String status,
            @Param("healthy") Boolean healthy,
            @Param("plant") String plant,
            @Param("plantKeys") List<String> plantKeys,
            @Param("disease") String disease,
            @Param("diseaseKeys") List<String> diseaseKeys,
            @Param("uploadedFrom") Instant uploadedFrom,
            @Param("uploadedTo") Instant uploadedTo,
            @Param("ascending") boolean ascending,
            Pageable pageable
    );

    Optional<Scan> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT scan
            FROM Scan scan
            LEFT JOIN FETCH scan.plot
            LEFT JOIN FETCH scan.followUpOf
            WHERE scan.id = :id AND scan.user.id = :userId
            """)
    Optional<Scan> findDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT new fr.agroscan.repository.ScanInsightsCounts(
                COUNT(scan.id),
                COALESCE(SUM(CASE WHEN scan.analysisStatus = :uploaded THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN scan.analysisStatus = :pending THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN scan.analysisStatus = :running THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN scan.analysisStatus = :done THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN scan.analysisStatus = :failed THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN scan.analysisHealthy = TRUE THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN scan.analysisHealthy = FALSE THEN 1 ELSE 0 END), 0)
            )
            FROM Scan scan
            WHERE scan.user.id = :userId
              AND scan.archived = FALSE
            """)
    ScanInsightsCounts insightsCounts(
            @Param("userId") Long userId,
            @Param("uploaded") ScanAnalysisStatus uploaded,
            @Param("pending") ScanAnalysisStatus pending,
            @Param("running") ScanAnalysisStatus running,
            @Param("done") ScanAnalysisStatus done,
            @Param("failed") ScanAnalysisStatus failed
    );

    @Query(value = """
            SELECT
                p.id AS plotId,
                COALESCE(p.name, NULLIF(TRIM(s.plot_name), ''), 'Sans parcelle') AS plotName,
                COUNT(s.id) AS total,
                COALESCE(SUM(CASE WHEN s.analysis_healthy = TRUE THEN 1 ELSE 0 END), 0) AS healthy,
                COALESCE(SUM(CASE WHEN s.analysis_healthy = FALSE THEN 1 ELSE 0 END), 0) AS diseased,
                MAX(s.uploaded_at) AS latestScanAt
            FROM scans s
            LEFT JOIN plots p ON p.id = s.plot_id
            WHERE s.user_id = :userId
              AND s.archived = FALSE
            GROUP BY p.id, COALESCE(p.name, NULLIF(TRIM(s.plot_name), ''), 'Sans parcelle')
            ORDER BY MAX(s.uploaded_at) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<PlotHealthRow> plotHealth(@Param("userId") Long userId, @Param("limit") int limit);

    @Query("""
            SELECT new fr.agroscan.repository.DiseaseSummary(
                scan.analysisDisease,
                COUNT(scan.id),
                MAX(scan.analyzedAt)
            )
            FROM Scan scan
            WHERE scan.user.id = :userId
              AND scan.archived = FALSE
              AND scan.analysisHealthy = FALSE
              AND scan.analysisDisease IS NOT NULL
            GROUP BY scan.analysisDisease
            ORDER BY COUNT(scan.id) DESC, MAX(scan.analyzedAt) DESC
            """)
    List<DiseaseSummary> frequentDiseases(@Param("userId") Long userId, Pageable pageable);
}
