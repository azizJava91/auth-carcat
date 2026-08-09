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
@Table(name = "consumed_auth_tokens")
public class ConsumedAuthToken {

    @Id
    @Column(length = 64)
    String tokenHash;

    LocalDateTime consumedAt;
}
