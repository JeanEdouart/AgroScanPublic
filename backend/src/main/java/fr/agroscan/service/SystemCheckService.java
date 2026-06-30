package fr.agroscan.service;

import fr.agroscan.repository.SystemCheckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemCheckService {

    private final SystemCheckRepository repository;

    public SystemCheckService(SystemCheckRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String readDatabaseMessage() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No system check row found"))
                .getMessage();
    }
}
