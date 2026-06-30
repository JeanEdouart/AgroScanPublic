package fr.agroscan.controller;

import fr.agroscan.service.AppNotificationService;
import fr.agroscan.service.AppNotificationService.NotificationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final AppNotificationService notificationService;

    public NotificationController(AppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    List<NotificationResponse> list(Authentication authentication) {
        return notificationService.list(authentication.getName());
    }

    @PutMapping("/{id}")
    NotificationResponse upsert(
            Authentication authentication,
            @PathVariable @Size(max = 120) String id,
            @Valid @RequestBody UpsertNotificationRequest request
    ) {
        return notificationService.upsert(
                authentication.getName(),
                id,
                request.label(),
                request.message(),
                request.progress(),
                request.tone(),
                request.scanId()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void dismiss(Authentication authentication, @PathVariable String id) {
        notificationService.dismiss(authentication.getName(), id);
    }

    @DeleteMapping("/finished")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clearFinished(Authentication authentication) {
        notificationService.clearFinished(authentication.getName());
    }

    record UpsertNotificationRequest(
            @NotBlank @Size(max = 160) String label,
            @NotBlank @Size(max = 500) String message,
            @Min(0) @Max(100) int progress,
            @NotBlank @Size(max = 20) String tone,
            Long scanId
    ) {
    }
}
