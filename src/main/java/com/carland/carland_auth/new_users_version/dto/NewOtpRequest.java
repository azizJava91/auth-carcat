package com.carland.carland_auth.new_users_version.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewOtpRequest {
    String authToken;
    String otp;
    String otpCode;
}
