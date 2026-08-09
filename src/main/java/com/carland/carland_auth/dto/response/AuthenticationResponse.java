package com.carland.carland_auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    /** Legacy field name. */
    String registerToken;
    /** Mid-migration alias — same value as registerToken when set via helper. */
    String authenticationToken;
    String message;

    public static AuthenticationResponse ofToken(String token, String message) {
        return AuthenticationResponse.builder()
                .registerToken(token)
                .authenticationToken(token)
                .message(message)
                .build();
    }
}
