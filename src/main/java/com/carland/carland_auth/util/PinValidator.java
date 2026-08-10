package com.carland.carland_auth.util;

import com.carland.carland_auth.exceptions.AuthApiException;
import org.springframework.http.HttpStatus;

import java.util.Set;

/**
 * NewUsers PIN rules (PO).
 */
public final class PinValidator {

    private static final Set<String> SEQUENCES = Set.of(
            "0123", "1234", "2345", "3456", "4567", "5678", "6789",
            "9876", "8765", "7654", "6543", "5432", "4321", "3210"
    );

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
        if (allSame || SEQUENCES.contains(pin)) {
            throw new AuthApiException("WEAK_PIN", "Please choose a less predictable PIN.", HttpStatus.BAD_REQUEST);
        }
    }
}
