package com.carland.carland_auth.service;

import com.carland.carland_auth.entity.IpOtpRateLimit;
import com.carland.carland_auth.entity.OtpRateLimit;
import com.carland.carland_auth.repository.IpOtpRateLimitRepository;
import com.carland.carland_auth.repository.OtpRateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists OTP send locks in a separate transaction so AuthApiException
 * rollback on createAndSend does not wipe lockedUntil (countdown must shrink).
 * Also resets send-window counters on lock / expired-lock so unlock grants a fresh quota
 * instead of immediately re-locking for another full period.
 */
@Service
@RequiredArgsConstructor
public class OtpSendLockService {

    private final OtpRateLimitRepository otpRateLimitRepository;
    private final IpOtpRateLimitRepository ipOtpRateLimitRepository;

    public record Result(LocalDateTime lockedUntil, long remainingSeconds) {
    }

    /**
     * If already locked, returns existing deadline (does not extend) and ensures
     * send counters are cleared so the next unlock is not an instant re-lock.
     * Otherwise sets phoneLockedUntil = now + lockMinutes, clears send window, commits.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result lockPhone(String phone, int lockMinutes) {
        LocalDateTime now = LocalDateTime.now();
        OtpRateLimit rate = otpRateLimitRepository.findByPhoneNumber(phone)
                .orElseGet(() -> OtpRateLimit.builder().phoneNumber(phone).sendCount(0).verifyFailCount(0).build());

        if (rate.getPhoneLockedUntil() != null && rate.getPhoneLockedUntil().isAfter(now)) {
            resetPhoneSendWindow(rate);
            otpRateLimitRepository.save(rate);
            long rem = Math.max(1, java.time.Duration.between(now, rate.getPhoneLockedUntil()).getSeconds());
            return new Result(rate.getPhoneLockedUntil(), rem);
        }

        LocalDateTime until = now.plusMinutes(lockMinutes);
        rate.setPhoneLockedUntil(until);
        resetPhoneSendWindow(rate);
        otpRateLimitRepository.save(rate);
        long rem = Math.max(1, java.time.Duration.between(now, until).getSeconds());
        return new Result(until, rem);
    }

    /**
     * If phone lock deadline has passed, clear lock + send window so createAndSend
     * does not immediately re-apply a full lock from a stale sendCount.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearExpiredPhoneLock(String phone) {
        LocalDateTime now = LocalDateTime.now();
        otpRateLimitRepository.findByPhoneNumber(phone).ifPresent(rate -> {
            if (rate.getPhoneLockedUntil() != null && !rate.getPhoneLockedUntil().isAfter(now)) {
                rate.setPhoneLockedUntil(null);
                resetPhoneSendWindow(rate);
                otpRateLimitRepository.save(rate);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result lockIp(String ip, int lockHours) {
        LocalDateTime now = LocalDateTime.now();
        IpOtpRateLimit rate = ipOtpRateLimitRepository.findByIpAddress(ip)
                .orElseGet(() -> IpOtpRateLimit.builder().ipAddress(ip).sendCount(0).build());

        if (rate.getLockedUntil() != null && rate.getLockedUntil().isAfter(now)) {
            resetIpSendWindow(rate);
            ipOtpRateLimitRepository.save(rate);
            long rem = Math.max(1, java.time.Duration.between(now, rate.getLockedUntil()).getSeconds());
            return new Result(rate.getLockedUntil(), rem);
        }

        LocalDateTime until = now.plusHours(lockHours);
        rate.setLockedUntil(until);
        resetIpSendWindow(rate);
        ipOtpRateLimitRepository.save(rate);
        long rem = Math.max(1, java.time.Duration.between(now, until).getSeconds());
        return new Result(until, rem);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearExpiredIpLock(String ip) {
        LocalDateTime now = LocalDateTime.now();
        ipOtpRateLimitRepository.findByIpAddress(ip).ifPresent(rate -> {
            if (rate.getLockedUntil() != null && !rate.getLockedUntil().isAfter(now)) {
                rate.setLockedUntil(null);
                resetIpSendWindow(rate);
                ipOtpRateLimitRepository.save(rate);
            }
        });
    }

    private static void resetPhoneSendWindow(OtpRateLimit rate) {
        rate.setSendCount(0);
        rate.setSendWindowStart(null);
    }

    private static void resetIpSendWindow(IpOtpRateLimit rate) {
        rate.setSendCount(0);
        rate.setSendWindowStart(null);
    }
}
