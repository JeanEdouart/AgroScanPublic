package fr.agroscan.controller;

import fr.agroscan.domain.AppUser;
import fr.agroscan.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthResponse.from(authService.register(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        ));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthResponse.from(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody TokenRequest request) {
        return AuthResponse.from(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    void logout(@Valid @RequestBody TokenRequest request) {
        authService.logout(request.refreshToken());
    }

    record RegisterRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password
    ) {
    }

    record TokenRequest(@NotBlank String refreshToken) {
    }

    record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        static AuthResponse from(AuthService.AuthResult result) {
            return new AuthResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    result.expiresIn(),
                    UserResponse.from(result.user())
            );
        }
    }

    record UserResponse(
            Long id,
            String email,
            String firstName,
            String lastName,
            String role,
            String profilePhotoDataUrl
    ) {
        static UserResponse from(AppUser user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().getName(),
                    user.getProfilePhotoDataUrl()
            );
        }
    }
}
