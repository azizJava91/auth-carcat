package com.carland.carland_auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteRequest {
    String phoneNumber;
    /** Legacy credential; {@code pin} still accepted. */
    @JsonAlias({"pin", "pinCode"})
    String password;
    String pin;
    String name;
    String surname;

    public String resolveCredential() {
        if (password != null && !password.isBlank()) {
            return password;
        }
        if (pin != null && !pin.isBlank()) {
            return pin;
        }
        return null;
    }
}
