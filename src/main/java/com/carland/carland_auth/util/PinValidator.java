package com.carland.carland_auth.util;

import com.carland.carland_auth.enums.EnumMessagesLangValues;
import com.carland.carland_auth.exceptions.MissingFieldException;
import com.carland.carland_auth.exceptions.WeakPinException;

import java.util.Set;

/**
 * Shared weak-PIN rules for legacy and newUsers flows.
 */
public final class PinValidator {

    private static final Set<String> SEQUENCES = Set.of(
            "0123", "1234", "2345", "3456", "4567", "5678", "6789",
            "9876", "8765", "7654", "6543", "5432", "4321", "3210"
    );

    private PinValidator() {
    }

    public static void validate(String pin, String acceptLanguage) {
        if (pin == null || pin.isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_USER_FIELDS.getMessageByLang(acceptLanguage));
        }
        if (!pin.matches("\\d{4}")) {
            throw new WeakPinException(EnumMessagesLangValues.WEAK_PIN.getMessageByLang(acceptLanguage));
        }
        char first = pin.charAt(0);
        boolean allSame = pin.chars().allMatch(c -> c == first);
        if (allSame || SEQUENCES.contains(pin)) {
            throw new WeakPinException(EnumMessagesLangValues.WEAK_PIN.getMessageByLang(acceptLanguage));
        }
    }
}
