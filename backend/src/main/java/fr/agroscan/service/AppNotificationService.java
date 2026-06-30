package fr.agroscan.service;

import fr.agroscan.domain.AppNotification;
import fr.agroscan.repository.AppNotificationRepository;
import fr.agroscan.repository.AppUserRepository;
import fr.agroscan.repository.ScanRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AppNotificationService {

    private static final String ACTIVE_TONE = "active";

    private final AppNotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final ScanRepository scanRepository;
    private final NotificationWebSocketBroadcaster broadcaster;
    private final EntityManager entityManager;

    public AppNotificationService(
            AppNotificationRepository notificationRepository,
            AppUserRepository userRepository,
            ScanRepository scanRepository,
            NotificationWebSocketBroadcaster broadcaster,
            EntityManager entityManager
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.scanRepository = scanRepository;
        this.broadcaster = broadcaster;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {
        return notificationRepository.findTop50ByUser_EmailIgnoreCaseOrderByUpdatedAtDesc(email).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse upsert(
            String email,
            String externalId,
            String label,
            String message,
            int progress,
            String tone,
            Long scanId
    ) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (scanId != null && scanRepository.findByIdAndUserId(scanId, user.getId()).isEmpty()) {
            throw new ResourceNotFoundException("Scan introuvable");
        }
        Long notificationId = upsertNotification(user.getId(), externalId, label, message, progress, tone, scanId);
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        NotificationResponse response = NotificationResponse.from(notification);
        broadcaster.send(email, new NotificationEvent("UPSERT", response));
        return response;
    }

    private Long upsertNotification(
            Long userId,
            String externalId,
            String label,
            String message,
            int progress,
            String tone,
            Long scanId
    ) {
        Number notificationId = (Number) entityManager.createNativeQuery("""
                        INSERT INTO notifications (
                            user_id,
                            external_id,
                            label,
                            message,
                            progress,
                            tone,
                            scan_id,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            :userId,
                            :externalId,
                            :label,
                            :message,
                            :progress,
                            :tone,
                            :scanId,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        ON CONFLICT ON CONSTRAINT uk_notifications_user_external
                        DO UPDATE SET
                            label = EXCLUDED.label,
                            message = EXCLUDED.message,
                            progress = EXCLUDED.progress,
                            tone = EXCLUDED.tone,
                            scan_id = EXCLUDED.scan_id,
                            updated_at = CURRENT_TIMESTAMP
                        RETURNING id
                        """)
                .setParameter("userId", userId)
                .setParameter("externalId", externalId)
                .setParameter("label", label)
                .setParameter("message", message)
                .setParameter("progress", Math.max(0, Math.min(100, progress)))
                .setParameter("tone", tone)
                .setParameter("scanId", scanId)
                .getSingleResult();
        return notificationId.longValue();
    }

    @Transactional
    public void dismiss(String email, String externalId) {
        notificationRepository.deleteByUser_EmailIgnoreCaseAndExternalId(email, externalId);
        broadcaster.send(email, new NotificationEvent("DELETE", new DeletedNotificationResponse(externalId)));
    }

    @Transactional
    public void clearFinished(String email) {
        notificationRepository.deleteByUser_EmailIgnoreCaseAndToneNot(email, ACTIVE_TONE);
        broadcaster.send(email, new NotificationEvent("REFRESH", list(email)));
    }

    public record NotificationResponse(
            String id,
            String label,
            String message,
            int progress,
            String tone,
            Long scanId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static NotificationResponse from(AppNotification notification) {
            return new NotificationResponse(
                    notification.getExternalId(),
                    notification.getLabel(),
                    notification.getMessage(),
                    notification.getProgress(),
                    notification.getTone(),
                    notification.getScanId(),
                    notification.getCreatedAt(),
                    notification.getUpdatedAt()
            );
        }
    }

    public record NotificationEvent(String type, Object payload) {
    }

    public record DeletedNotificationResponse(String id) {
    }
}
