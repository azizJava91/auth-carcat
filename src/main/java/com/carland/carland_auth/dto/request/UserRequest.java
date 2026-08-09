package com.carland.carland_auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import lombok.experimental.FieldDefaults;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)

public class UserRequest {

    String phoneNumber;

    /**
     * Legacy credential field. Clients may also send {@code pin} / {@code pinCode}.
     */
    @JsonAlias({"pin", "pinCode"})
    String password;

    /** Optional alias kept for mid-migration clients; prefer {@link #password}. */
    String pin;

    /** New-users PIN field; also accepted on shared DTO. */
    String pinCode;

    String name;
    String surname;

    /** Same value as carland_service device-tokens {@code deviceToken} (FCM). */
    @JsonAlias({"deviceId"})
    String deviceToken;

    String platform;

    /** New auth flow: REGISTER / RESET (optional). */
    String purpose;

    /** New auth flow: body auth token (optional on legacy). */
    @JsonAlias({"authenticationToken", "registerToken"})
    String authToken;

    /** Resolve credential from password / pin / pinCode (first non-blank). */
    public String resolveCredential() {
        if (password != null && !password.isBlank()) {
            return password;
        }
        if (pin != null && !pin.isBlank()) {
            return pin;
        }
        if (pinCode != null && !pinCode.isBlank()) {
            return pinCode;
        }
        return null;
    }
}
