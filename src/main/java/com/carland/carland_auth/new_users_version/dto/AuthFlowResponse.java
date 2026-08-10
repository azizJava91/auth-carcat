package com.carland.carland_auth.new_users_version.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthFlowResponse {
    /** From /auth and /otp/createAndSend */
    String authToken;
    /** From /otp/verify — used in setPinCode body */
    String pinSetupToken;
    /** SEND_OTP | PIN_CHECK | VERIFY_OTP | SET_PIN */
    String next;
    String message;
    String purpose;
}
