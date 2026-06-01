package com.carland.carland_auth.jwt;

import com.carland.carland_auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
public class JWTService {

    @Value("${access.token.secret-key}")
    private String accessTokenSecretKey;

    @Value("${register.token.secret-key}")
    private String registerTokenSecretKey;

    @Value("${refresh.token.secret-key}")
    private String refreshTokenSecretKey;

    @Value("${carland.issuer.key}")
    private String issuerKey;






    public String generateAccessToken(User user, Long expirationTime) {
        return Jwts.builder()
                .subject(user.getPhoneNumber())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getName())
                .claim("surname", user.getSurname())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(expirationTime)))
                .signWith(getSignKey(accessTokenSecretKey))
                .issuer(issuerKey)
                .compact();
    }
    public String generateRefreshToken(User user, Long expirationTime) {
        return Jwts.builder()
                .subject(user.getPhoneNumber())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(expirationTime)))
                .signWith(getSignKey(refreshTokenSecretKey))
                .issuer(issuerKey)
                .compact();
    }

    public String generateRegisterToken(User user, Long expirationTime) {
        return Jwts.builder()
                .claim("userId", user.getId())
                .claim("type", "REGISTER")
                .subject("user-registration")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(expirationTime)))
                .signWith(getSignKey(registerTokenSecretKey))
                .compact();
    }


    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class), accessTokenSecretKey);
    }

    public Long extractUserIdFromRefreshToken(String token) {
        return extractClaim(token.substring(7).trim(), claims -> claims.get("userId", Long.class), refreshTokenSecretKey);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject, accessTokenSecretKey);
    }



    public Long extractUserIdFromRegisterToken(String token) {
        return extractClaim(token.substring(7).trim(), claims -> claims.get("userId", Long.class), registerTokenSecretKey);
    }


    public boolean isAccessTokenValid(String token) {
        return !isTokenExpired(token, accessTokenSecretKey);
    }
    public boolean isRegisterTokenValid(String token) {
        return !isTokenExpired(token, registerTokenSecretKey);
    }
    public boolean isRefreshTokenValid(String token) {
        return !isTokenExpired(token, refreshTokenSecretKey);
    }


    private boolean isTokenExpired(String token, String key) {
        try {
            Date exp = extractClaim(token, Claims::getExpiration, key);
            return exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, String key) {
        Claims claims = Jwts.parser()
                .verifyWith(getSignKey(key))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claimsResolver.apply(claims);
    }

    private SecretKey getSignKey(String key) {
        return Keys.hmacShaKeyFor(key.getBytes());
    }

    public String extractUserRoleFromRegisterToken(String token) {
        return extractClaim(token.substring(7).trim(), claims -> claims.get("role", String.class), registerTokenSecretKey);
    }
    public String extractUserRoleFromAccessToken(String token) {
        return extractClaim(token.substring(7).trim(), claims -> claims.get("role", String.class), accessTokenSecretKey);
    }
}

