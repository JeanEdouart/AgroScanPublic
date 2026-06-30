package fr.agroscan.repository;

import fr.agroscan.domain.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT new fr.agroscan.repository.AdminUserSummary(
                user.id, user.email, user.firstName, user.lastName, user.role.name,
                user.enabled, user.createdAt, user.updatedAt
            )
            FROM AppUser user
            WHERE LOWER(user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(user.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(user.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(user.role.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<AdminUserSummary> searchForAdmin(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    Optional<AppUser> findById(Long id);

    long countByRoleName(String roleName);
}
