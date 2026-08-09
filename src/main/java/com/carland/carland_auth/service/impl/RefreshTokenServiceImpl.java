package com.carland.carland_auth.service.impl;

import com.carland.carland_auth.entity.RefreshToken;
import com.carland.carland_auth.entity.User;
import com.carland.carland_auth.jwt.JWTService;
import com.carland.carland_auth.service.interfaces.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JWTService jwtService;
    @Value("${refresh.token.expiration}")
    private Long REFRESH_TOKEN_EXPIRATION_SECONDS;


    @Override
    public RefreshToken createRefreshToken(User user) {
        return createRefreshToken(user, null, null);
    }

    @Override
    public RefreshToken createRefreshToken(User user, String deviceId, String platform) {
        return RefreshToken.builder()
                .token(jwtService.generateRefreshToken(user, REFRESH_TOKEN_EXPIRATION_SECONDS))
                .createdAt(LocalDateTime.now())
                .deviceId(deviceId)
                .platform(platform)
                .build();
    }
}
