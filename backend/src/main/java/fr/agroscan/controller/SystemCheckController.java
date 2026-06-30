package fr.agroscan.controller;

import fr.agroscan.service.SystemCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/system-check")
public class SystemCheckController {

    private final SystemCheckService service;

    public SystemCheckController(SystemCheckService service) {
        this.service = service;
    }

    @GetMapping
    SystemCheckResponse check() {
        return new SystemCheckResponse(
                "UP",
                "UP",
                service.readDatabaseMessage(),
                Instant.now()
        );
    }

    record SystemCheckResponse(String backend, String database, String message, Instant checkedAt) {
    }
}
