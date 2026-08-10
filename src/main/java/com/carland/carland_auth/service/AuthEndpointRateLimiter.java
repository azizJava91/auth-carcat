package com.carland.carland_auth.service;

import com.carland.carland_auth.exceptions.AuthApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple sliding-window rate limit for POST /newUsers/auth (phone + IP).
 */
@Service
public class AuthEndpointRateLimiter {

    @Value("${auth.rate-limit.phone-per-minute:10}")
    private int phonePerMinute;

    @Value("${auth.rate-limit.ip-per-minute:30}")
    private int ipPerMinute;

    private final Map<String, List<Long>> phoneHits = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> ipHits = new ConcurrentHashMap<>();

    public void check(String phone, String ip) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000L;
        if (!allow(phoneHits, phone == null ? "unknown" : phone, now, windowStart, phonePerMinute)) {
            throw new AuthApiException("LOGIN_LOCKED", "Too many attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!allow(ipHits, ip == null ? "unknown" : ip, now, windowStart, ipPerMinute)) {
            throw new AuthApiException(null, "Too many attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private boolean allow(Map<String, List<Long>> store, String key, long now, long windowStart, int max) {
        List<Long> times = store.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (times) {
            times.removeIf(t -> t < windowStart);
            if (times.size() >= max) {
                return false;
            }
            times.add(now);
            return true;
        }
    }
}
