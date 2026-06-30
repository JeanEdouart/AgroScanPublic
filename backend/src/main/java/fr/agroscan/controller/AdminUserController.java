package fr.agroscan.controller;

import fr.agroscan.domain.AppUser;
import fr.agroscan.repository.AdminUserSummary;
import fr.agroscan.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    PageResponse<UserResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean ascending
    ) {
        Page<AdminUserSummary> users = adminUserService.search(search, page, size, sortBy, ascending);
        return PageResponse.from(users.map(UserResponse::from));
    }

    @PatchMapping("/{id}")
    UserResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(adminUserService.update(
                authentication.getName(),
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.role(),
                request.enabled()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication authentication, @PathVariable Long id) {
        adminUserService.delete(authentication.getName(), id);
    }

    record UpdateUserRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Pattern(regexp = "USER|ADMIN") String role,
            boolean enabled
    ) {
    }

    record UserResponse(
            Long id,
            String email,
            String firstName,
            String lastName,
            String role,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        static UserResponse from(AdminUserSummary user) {
            return new UserResponse(
                    user.id(), user.email(), user.firstName(), user.lastName(), user.role(),
                    user.enabled(), user.createdAt(), user.updatedAt()
            );
        }

        static UserResponse from(AppUser user) {
            return new UserResponse(
                    user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().getName(),
                    user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt()
            );
        }
    }

    record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
            );
        }
    }
}
