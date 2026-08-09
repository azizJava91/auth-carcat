package com.carland.carland_auth.new_users_version.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthFlowResponse {
    String authToken;
    /** SEND_OTP | PIN_CHECK | SET_PIN */
    String next;
    String message;
    String purpose;
}
