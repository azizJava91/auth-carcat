package com.carland.carland_auth.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)

public class UserRequest {

    String phoneNumber;

//    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{8,}$",
//            message = "Şifrə ən az 8 simvoldan ibarət olmalı, tərkibində böyük hərf, kiçik hərf, rəqəm və simvol olmalıdır")
    String password;
    String name;
    String surname;
}
