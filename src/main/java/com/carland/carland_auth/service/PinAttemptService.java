package com.carland.carland_auth.service;

import com.carland.carland_auth.entity.User;
import com.carland.carland_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists wrong-PIN counters in a separate committed transaction so that
 * subsequent WrongPasswordException / PinLockedException rollbacks do not undo the count.
 */
@Service
@RequiredArgsConstructor
public class PinAttemptService {

    @Value("${pin.max-attempts:3}")
    private int pinMaxAttempts;

    @Value("${pin.attempt-window-minutes:10}")
    private int pinAttemptWindowMinutes;

    @Value("${pin.lock-duration-minutes:5}")
    private int pinLockDurationMinutes;

    private final UserRepository userRepository;

    public record Result(boolean locked, LocalDateTime lockedUntil, long remainingSeconds) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result recordWrongPin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        int attempts;
        if (user.getLastFailedPinAt() == null
                || user.getLastFailedPinAt().isBefore(now.minusMinutes(pinAttemptWindowMinutes))) {
            attempts = 1;
        } else {
            int current = user.getFailedPinAttempts() == null ? 0 : user.getFailedPinAttempts();
            attempts = current + 1;
        }

        user.setFailedPinAttempts(attempts);
        user.setLastFailedPinAt(now);

        if (attempts >= pinMaxAttempts) {
            LocalDateTime until = now.plusMinutes(pinLockDurationMinutes);
            user.setPinLockedUntil(until);
            user.setFailedPinAttempts(0);
            userRepository.save(user);
            long remaining = Math.max(1, java.time.Duration.between(now, until).getSeconds());
            return new Result(true, until, remaining);
        }

        userRepository.save(user);
        return new Result(false, null, 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearFailureState(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setFailedPinAttempts(0);
        user.setLastFailedPinAt(null);
        user.setPinLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearExpiredLock(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        if (user.getPinLockedUntil() != null && !user.getPinLockedUntil().isAfter(now)) {
            user.setPinLockedUntil(null);
            userRepository.save(user);
        }
    }
}
