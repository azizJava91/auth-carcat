package com.carland.carland_auth.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CarlandPrincipal {

    private final Long userId;
    private final String phoneNumber;
}
