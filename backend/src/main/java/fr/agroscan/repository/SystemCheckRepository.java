package fr.agroscan.repository;

import fr.agroscan.domain.SystemCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemCheckRepository extends JpaRepository<SystemCheck, Long> {

    Optional<SystemCheck> findFirstByOrderByIdAsc();
}
