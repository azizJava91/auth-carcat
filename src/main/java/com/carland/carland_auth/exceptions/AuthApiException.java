package com.carland.carland_auth.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Named API errors for NewUsers / Flutter contract (PO error codes).
 */
@Getter
public class AuthApiException extends RuntimeException {

    private final String error;
    private final HttpStatus status;
    private final LocalDateTime lockedUntil;
    private final Long remainingSeconds;

    public AuthApiException(String error, String message, HttpStatus status) {
        this(error, message, status, null, null);
    }

    public AuthApiException(String error, String message, HttpStatus status,
                            LocalDateTime lockedUntil, Long remainingSeconds) {
        super(message);
        this.error = error;
        this.status = status;
        this.lockedUntil = lockedUntil;
        this.remainingSeconds = remainingSeconds;
    }
}
