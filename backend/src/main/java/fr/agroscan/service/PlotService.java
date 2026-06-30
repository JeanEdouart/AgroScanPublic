package fr.agroscan.service;

import fr.agroscan.domain.Plot;
import fr.agroscan.repository.AppUserRepository;
import fr.agroscan.repository.PlotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlotService {

    private final PlotRepository plotRepository;
    private final AppUserRepository userRepository;

    public PlotService(PlotRepository plotRepository, AppUserRepository userRepository) {
        this.plotRepository = plotRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Plot> list(String email) {
        return plotRepository.findByUser_EmailIgnoreCaseOrderByNameAsc(email);
    }

    @Transactional(readOnly = true)
    public List<Plot> search(String email, String search, int limit) {
        return plotRepository.search(email, search == null ? "" : search.trim(), PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));
    }

    @Transactional
    public Plot create(String email, String name, String description) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return plotRepository.save(new Plot(user, name, description));
    }

    @Transactional
    public Plot update(String email, Long id, String name, String description) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Plot plot = plotRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcelle introuvable"));
        plot.update(name, description);
        return plot;
    }

    @Transactional
    public void delete(String email, Long id) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Plot plot = plotRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parcelle introuvable"));
        plotRepository.delete(plot);
    }
}
