package fr.agroscan.service;

import fr.agroscan.domain.AppUser;
import fr.agroscan.domain.RefreshToken;
import fr.agroscan.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration accessTokenDuration;
    private final Duration refreshTokenDuration;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${agroscan.security.access-token-minutes}") long accessTokenMinutes,
            @Value("${agroscan.security.refresh-token-days}") long refreshTokenDays
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenDuration = Duration.ofMinutes(accessTokenMinutes);
        this.refreshTokenDuration = Duration.ofDays(refreshTokenDays);
    }

    String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("agroscan")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenDuration))
                .subject(user.getEmail())
                .claim("role", user.getRole().getName())
                .claim("name", user.getFirstName() + " " + user.getLastName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Transactional
    String createRefreshToken(AppUser user) {
        byte[] value = new byte[48];
        secureRandom.nextBytes(value);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        refreshTokenRepository.save(new RefreshToken(hash(rawToken), user, Instant.now().plus(refreshTokenDuration)));
        return rawToken;
    }

    @Transactional(readOnly = true)
    RefreshToken requireActiveRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException("Jeton de renouvellement invalide"));
        if (!token.isActive()) {
            throw new InvalidCredentialsException("Jeton de renouvellement expiré ou révoqué");
        }
        return token;
    }

    @Transactional
    void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
    }

    long accessTokenSeconds() {
        return accessTokenDuration.toSeconds();
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
