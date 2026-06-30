package fr.agroscan.controller;

import fr.agroscan.domain.AppUser;
import fr.agroscan.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    UserResponse me(Authentication authentication) {
        return UserResponse.from(authService.currentUser(authentication.getName()));
    }

    @PatchMapping
    ProfileResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        var result = authService.updateProfile(
                authentication.getName(),
                request.firstName(),
                request.lastName(),
                request.email()
        );
        return new ProfileResponse(result.accessToken(), UserResponse.from(result.user()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
    }

    @PutMapping("/profile-photo")
    UserResponse updateProfilePhoto(Authentication authentication, @Valid @RequestBody UpdateProfilePhotoRequest request) {
        return UserResponse.from(authService.updateProfilePhoto(
                authentication.getName(),
                request.imageBase64(),
                request.imageMediaType()
        ));
    }

    @DeleteMapping("/profile-photo")
    UserResponse clearProfilePhoto(Authentication authentication) {
        return UserResponse.from(authService.clearProfilePhoto(authentication.getName()));
    }

    record UpdateProfileRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 320) String email
    ) {
    }

    record ChangePasswordRequest(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    record UpdateProfilePhotoRequest(
            @NotBlank @Size(max = 700_000) String imageBase64,
            @NotBlank @Size(max = 40) String imageMediaType
    ) {
    }

    record ProfileResponse(String accessToken, UserResponse user) {
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
