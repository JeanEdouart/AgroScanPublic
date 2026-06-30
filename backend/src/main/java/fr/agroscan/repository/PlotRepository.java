package fr.agroscan.repository;

import fr.agroscan.domain.Plot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlotRepository extends JpaRepository<Plot, Long> {

    List<Plot> findByUser_EmailIgnoreCaseOrderByNameAsc(String email);

    @Query("""
            SELECT plot
            FROM Plot plot
            WHERE LOWER(plot.user.email) = LOWER(:email)
              AND LOWER(plot.name) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY plot.name ASC
            """)
    List<Plot> search(@Param("email") String email, @Param("search") String search, Pageable pageable);

    Optional<Plot> findByIdAndUserId(Long id, Long userId);
}
