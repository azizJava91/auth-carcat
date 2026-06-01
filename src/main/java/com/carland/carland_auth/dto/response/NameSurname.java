package com.carland.carland_auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NameSurname {
    String name;
    String surname;
    String phoneNumber;
}
