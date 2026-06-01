package com.carland.carland_auth.enums;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.Getter;

@Getter

public enum UserRoles {
    SUPER_ADMIN,
    ADMIN,
    USER,
    BOSS;
}
