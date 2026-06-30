package fr.agroscan.service;

import fr.agroscan.domain.AppUser;
import fr.agroscan.domain.RefreshToken;
import fr.agroscan.repository.AppUserRepository;
import fr.agroscan.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final long MAX_PROFILE_PHOTO_BYTES = 512 * 1024;

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ImageValidationService imageValidationService;

    public AuthService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            ImageValidationService imageValidationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.imageValidationService = imageValidationService;
    }

    @Transactional
    public AuthResult register(String email, String password, String firstName, String lastName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }
        var role = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role is missing"));
        AppUser user = userRepository.save(new AppUser(
                normalizedEmail,
                passwordEncoder.encode(password),
                firstName.trim(),
                lastName.trim(),
                role
        ));
        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou mot de passe incorrect"));
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("E-mail ou mot de passe incorrect");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        RefreshToken refreshToken = tokenService.requireActiveRefreshToken(refreshTokenValue);
        refreshToken.revoke();
        return issueTokens(refreshToken.getUser());
    }

    @Transactional(readOnly = true)
    public AppUser currentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur introuvable"));
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    @Transactional
    public ProfileResult updateProfile(String currentEmail, String firstName, String lastName, String email) {
        AppUser user = currentUser(currentEmail);
        String normalizedEmail = normalizeEmail(email);
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }
        user.updateProfile(firstName.trim(), lastName.trim(), normalizedEmail);
        return new ProfileResult(tokenService.createAccessToken(user), user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        AppUser user = currentUser(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CurrentPasswordInvalidException();
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        tokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public AppUser updateProfilePhoto(String email, String imageBase64, String imageMediaType) {
        AppUser user = currentUser(email);
        var image = imageValidationService.validate(
                imageBase64,
                imageMediaType,
                MAX_PROFILE_PHOTO_BYTES,
                "La photo de profil doit peser moins de 512 Ko"
        );
        user.updateProfilePhoto(imageBase64, image.mediaType(), image.bytes().length);
        return user;
    }

    @Transactional
    public AppUser clearProfilePhoto(String email) {
        AppUser user = currentUser(email);
        user.clearProfilePhoto();
        return user;
    }

    private AuthResult issueTokens(AppUser user) {
        return new AuthResult(
                tokenService.createAccessToken(user),
                tokenService.createRefreshToken(user),
                tokenService.accessTokenSeconds(),
                user
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthResult(String accessToken, String refreshToken, long expiresIn, AppUser user) {
    }

    public record ProfileResult(String accessToken, AppUser user) {
    }
}
