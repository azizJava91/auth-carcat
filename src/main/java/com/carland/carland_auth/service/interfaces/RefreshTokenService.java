package com.carland.carland_auth.service.interfaces;

import com.carland.carland_auth.entity.RefreshToken;
import com.carland.carland_auth.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    RefreshToken createRefreshToken(User user, String deviceId, String platform);
}
