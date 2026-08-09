package com.carland.carland_auth.entity;

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
@Table(name = "otp_rate_limits")
public class OtpRateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String phoneNumber;

    String lastIp;

    @Builder.Default
    Integer sendCount = 0;

    LocalDateTime sendWindowStart;
    LocalDateTime lastSentAt;
    LocalDateTime phoneLockedUntil;

    @Builder.Default
    Integer verifyFailCount = 0;

    LocalDateTime verifyLockedUntil;
}
