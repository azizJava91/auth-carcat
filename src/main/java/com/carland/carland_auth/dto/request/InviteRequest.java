package com.carland.carland_auth.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteRequest {
    String phoneNumber;
    String password;
    String name;
    String surname;

}
