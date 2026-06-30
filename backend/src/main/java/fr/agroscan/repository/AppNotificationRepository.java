package fr.agroscan.repository;

import fr.agroscan.domain.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findTop50ByUser_EmailIgnoreCaseOrderByUpdatedAtDesc(String email);

    Optional<AppNotification> findByUser_EmailIgnoreCaseAndExternalId(String email, String externalId);

    void deleteByUser_EmailIgnoreCaseAndExternalId(String email, String externalId);

    void deleteByUser_EmailIgnoreCaseAndToneNot(String email, String tone);
}
