package com.carland.carland_auth.util;

import com.carland.carland_auth.exceptions.AuthApiException;
import org.springframework.http.HttpStatus;

/**
 * NewUsers PIN rules (PO): exactly 4 digits; all-same digits are weak.
 * Sequences like 1234 / 3456 are allowed.
 */
public final class PinValidator {

    private PinValidator() {
    }

    public static void validateNewUsersPin(String pin) {
        if (pin == null || pin.isBlank()) {
            throw new AuthApiException("PIN_LENGTH_ERROR", "PIN must be exactly 4 digits.", HttpStatus.BAD_REQUEST);
        }
        if (!pin.matches("\\d{4}")) {
            throw new AuthApiException("PIN_LENGTH_ERROR", "PIN must be exactly 4 digits.", HttpStatus.BAD_REQUEST);
        }
        char first = pin.charAt(0);
        boolean allSame = pin.chars().allMatch(c -> c == first);
        if (allSame) {
            throw new AuthApiException("WEAK_PIN", "Please choose a less predictable PIN.", HttpStatus.BAD_REQUEST);
        }
    }
}
