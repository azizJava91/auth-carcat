package com.carland.carland_auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String phoneNumber;

    /**
     * BCrypt hash of the 4-digit PIN.
     * Java field name is {@code pin}; DB column remains {@code password} for legacy compatibility.
     * When the old API is fully deprecated, migrate with:
     * {@code ALTER TABLE users RENAME COLUMN password TO pin;}
     */
    @Column(name = "password", nullable = false)
    String pin;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    List<RefreshToken> refreshTokens = new ArrayList<>();
    LocalDateTime createdAt;
    String status;
    String role;
    String name;
    String surname;

    /** Consecutive wrong PIN attempts within the attempt window. */
    @Builder.Default
    Integer failedPinAttempts = 0;

    /** End of temporary PIN lock (null = not locked). */
    LocalDateTime pinLockedUntil;

    /** Timestamp of last failed PIN attempt (for attempt window). */
    LocalDateTime lastFailedPinAt;
}
