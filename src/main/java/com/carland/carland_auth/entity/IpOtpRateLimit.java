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
@Table(name = "ip_otp_rate_limits")
public class IpOtpRateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String ipAddress;

    @Builder.Default
    Integer sendCount = 0;

    LocalDateTime sendWindowStart;
    LocalDateTime lockedUntil;
}
