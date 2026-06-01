package com.carland.carland_auth.enums;

import lombok.Getter;


@Getter
public enum UserStatus {
    INVITED,
    OTP_PENDING,
    OTP_VERIFIED,
    ACTIVE,
    BLOCKED,
    DELETED
}
