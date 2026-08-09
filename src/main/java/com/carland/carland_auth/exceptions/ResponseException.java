package com.carland.carland_auth.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseException {
    private String error;
    private String message;
    private LocalDateTime timeStamp;
    private Integer status;
    /** Present when PIN/login is temporarily locked (HTTP 429). */
    private LocalDateTime lockedUntil;
    /** Seconds left until unlock — for Flutter countdown / Retry-After. */
    private Long remainingSeconds;
    /** Alias used by PO contract (same as remainingSeconds). */
    private Long retryAfter;
}
