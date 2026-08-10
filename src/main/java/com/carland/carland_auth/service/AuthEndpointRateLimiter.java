package com.carland.carland_auth.service;

import com.carland.carland_auth.exceptions.AuthApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple sliding-window rate limit for POST /newUsers/auth (phone + IP).
 */
@Service
public class AuthEndpointRateLimiter {

    private static final long WINDOW_MS = 60_000L;

    @Value("${auth.rate-limit.phone-per-minute:10}")
    private int phonePerMinute;

    @Value("${auth.rate-limit.ip-per-minute:30}")
    private int ipPerMinute;

    private final Map<String, List<Long>> phoneHits = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> ipHits = new ConcurrentHashMap<>();

    public void check(String phone, String ip) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_MS;
        rejectIfLimited(phoneHits, phone == null ? "unknown" : phone, now, windowStart, phonePerMinute);
        rejectIfLimited(ipHits, ip == null ? "unknown" : ip, now, windowStart, ipPerMinute);
    }

    private void rejectIfLimited(Map<String, List<Long>> store, String key, long now, long windowStart, int max) {
        List<Long> times = store.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (times) {
            times.removeIf(t -> t < windowStart);
            if (times.size() >= max) {
                long oldest = times.stream().mapToLong(Long::longValue).min().orElse(now);
                long unlockAtMs = oldest + WINDOW_MS;
                long rem = Math.max(1, (unlockAtMs - now + 999) / 1000);
                LocalDateTime lockedUntil = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(unlockAtMs), ZoneId.systemDefault());
                throw new AuthApiException("LOGIN_LOCKED",
                        "Too many attempts. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS, lockedUntil, rem);
            }
            times.add(now);
        }
    }
}
