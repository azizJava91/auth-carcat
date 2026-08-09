package com.carland.carland_auth.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PinLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;
    private final long remainingSeconds;

    public PinLockedException(String message, LocalDateTime lockedUntil, long remainingSeconds) {
        super(message);
        this.lockedUntil = lockedUntil;
        this.remainingSeconds = remainingSeconds;

    }
}
