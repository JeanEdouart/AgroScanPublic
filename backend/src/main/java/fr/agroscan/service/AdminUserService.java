package fr.agroscan.service;

import fr.agroscan.domain.AppUser;
import fr.agroscan.repository.AdminUserSummary;
import fr.agroscan.repository.AppUserRepository;
import fr.agroscan.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class AdminUserService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "firstName", "firstName",
            "lastName", "lastName",
            "email", "email",
            "role", "role.name",
            "createdAt", "createdAt"
    );

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;

    public AdminUserService(AppUserRepository userRepository, RoleRepository roleRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummary> search(String search, int page, int size, String sortBy, boolean ascending) {
        String field = SORT_FIELDS.getOrDefault(sortBy, "createdAt");
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, field);
        return userRepository.searchForAdmin(search == null ? "" : search.trim(), PageRequest.of(page, Math.min(size, 50), sort));
    }

    @Transactional
    public AppUser update(
            String adminEmail,
            Long userId,
            String firstName,
            String lastName,
            String email,
            String roleName,
            boolean enabled
    ) {
        AppUser user = requireUser(userId);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new AdminOperationException("Modifiez votre propre compte depuis la page Mon profil");
        }
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }
        if ("ADMIN".equals(user.getRole().getName()) && !"ADMIN".equals(roleName)
                && userRepository.countByRoleName("ADMIN") <= 1) {
            throw new AdminOperationException("Le dernier administrateur ne peut pas perdre son rôle");
        }
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AdminOperationException("Rôle utilisateur invalide"));
        user.updateByAdmin(firstName.trim(), lastName.trim(), normalizedEmail, role, enabled);
        tokenService.revokeAllForUser(user.getId());
        return user;
    }

    @Transactional
    public void delete(String adminEmail, Long userId) {
        AppUser user = requireUser(userId);
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new AdminOperationException("Vous ne pouvez pas supprimer votre propre compte");
        }
        if ("ADMIN".equals(user.getRole().getName()) && userRepository.countByRoleName("ADMIN") <= 1) {
            throw new AdminOperationException("Le dernier administrateur ne peut pas être supprimé");
        }
        userRepository.delete(user);
    }

    private AppUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
