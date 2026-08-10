package com.carland.carland_auth.service;

import com.carland.carland_auth.entity.OtpRateLimit;
import com.carland.carland_auth.repository.OtpRateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpVerifyAttemptService {

    @Value("${otp.new.max-verify-attempts:3}")
    private int maxVerifyAttempts;

    @Value("${otp.new.verify-lock-minutes:5}")
    private int verifyLockMinutes;

    private final OtpRateLimitRepository otpRateLimitRepository;

    public record Result(boolean locked, LocalDateTime lockedUntil, long remainingSeconds) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result recordWrongVerify(String phone) {
        LocalDateTime now = LocalDateTime.now();
        OtpRateLimit rate = otpRateLimitRepository.findByPhoneNumber(phone)
                .orElseGet(() -> OtpRateLimit.builder().phoneNumber(phone).sendCount(0).verifyFailCount(0).build());

        if (rate.getVerifyLockedUntil() != null && rate.getVerifyLockedUntil().isAfter(now)) {
            long rem = Math.max(1, java.time.Duration.between(now, rate.getVerifyLockedUntil()).getSeconds());
            return new Result(true, rate.getVerifyLockedUntil(), rem);
        }

        int fails = (rate.getVerifyFailCount() == null ? 0 : rate.getVerifyFailCount()) + 1;
        rate.setVerifyFailCount(fails);
        if (fails >= maxVerifyAttempts) {
            LocalDateTime until = now.plusMinutes(verifyLockMinutes);
            rate.setVerifyLockedUntil(until);
            rate.setVerifyFailCount(0);
            otpRateLimitRepository.save(rate);
            long rem = Math.max(1, java.time.Duration.between(now, until).getSeconds());
            return new Result(true, until, rem);
        }
        otpRateLimitRepository.save(rate);
        return new Result(false, null, 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearVerifyCounters(String phone) {
        otpRateLimitRepository.findByPhoneNumber(phone).ifPresent(rate -> {
            rate.setVerifyFailCount(0);
            rate.setVerifyLockedUntil(null);
            rate.setSendCount(0);
            rate.setSendWindowStart(null);
            rate.setPhoneLockedUntil(null);
            otpRateLimitRepository.save(rate);
        });
    }
}
