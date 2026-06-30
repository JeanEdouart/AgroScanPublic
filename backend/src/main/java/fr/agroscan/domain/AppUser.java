package fr.agroscan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "profile_photo_base64", columnDefinition = "TEXT")
    private String profilePhotoBase64;

    @Column(name = "profile_photo_media_type", length = 40)
    private String profilePhotoMediaType;

    @Column(name = "profile_photo_size_bytes")
    private Long profilePhotoSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String email, String passwordHash, String firstName, String lastName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getProfilePhotoBase64() {
        return profilePhotoBase64;
    }

    public String getProfilePhotoMediaType() {
        return profilePhotoMediaType;
    }

    public Long getProfilePhotoSizeBytes() {
        return profilePhotoSizeBytes;
    }

    public String getProfilePhotoDataUrl() {
        if (profilePhotoBase64 == null || profilePhotoMediaType == null) {
            return null;
        }
        return "data:" + profilePhotoMediaType + ";base64," + profilePhotoBase64;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }

    public void updateProfilePhoto(String imageBase64, String mediaType, long sizeBytes) {
        this.profilePhotoBase64 = imageBase64;
        this.profilePhotoMediaType = mediaType;
        this.profilePhotoSizeBytes = sizeBytes;
        this.updatedAt = Instant.now();
    }

    public void clearProfilePhoto() {
        this.profilePhotoBase64 = null;
        this.profilePhotoMediaType = null;
        this.profilePhotoSizeBytes = null;
        this.updatedAt = Instant.now();
    }

    public void updateByAdmin(String firstName, String lastName, String email, Role role, boolean enabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }
}
