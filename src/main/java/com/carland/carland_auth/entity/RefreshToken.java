package com.carland.carland_auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    LocalDateTime createdAt;

    /**
     * FCM / client device token (same value mobile sends to carland_service /device-tokens).
     * Nullable for legacy logins that omit it.
     */
    @Column(name = "device_id")
    String deviceId;

    /** IOS / ANDROID (or whatever client sends). Nullable for legacy. */
    String platform;

    /** Soft revoke — row kept for login history; null = still usable. */
    @Column(name = "revoked_at")
    LocalDateTime revokedAt;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}
