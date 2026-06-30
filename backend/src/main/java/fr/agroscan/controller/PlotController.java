package fr.agroscan.controller;

import fr.agroscan.domain.Plot;
import fr.agroscan.service.PlotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

@RestController
@RequestMapping("/api/plots")
public class PlotController {

    private final PlotService plotService;

    public PlotController(PlotService plotService) {
        this.plotService = plotService;
    }

    @GetMapping
    List<PlotResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return plotService.search(authentication.getName(), search, limit).stream()
                .map(PlotResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlotResponse create(Authentication authentication, @Valid @RequestBody PlotRequest request) {
        return PlotResponse.from(plotService.create(authentication.getName(), request.name(), request.description()));
    }

    @PatchMapping("/{id}")
    PlotResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody PlotRequest request) {
        return PlotResponse.from(plotService.update(authentication.getName(), id, request.name(), request.description()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication authentication, @PathVariable Long id) {
        plotService.delete(authentication.getName(), id);
    }

    record PlotRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 1000) String description
    ) {
    }

    record PlotResponse(Long id, String name, String description, Instant createdAt) {
        static PlotResponse from(Plot plot) {
            return new PlotResponse(plot.getId(), plot.getName(), plot.getDescription(), plot.getCreatedAt());
        }
    }
}
