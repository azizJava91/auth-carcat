package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.OtpRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRateLimitRepository extends JpaRepository<OtpRateLimit, Long> {
    Optional<OtpRateLimit> findByPhoneNumber(String phoneNumber);
}
