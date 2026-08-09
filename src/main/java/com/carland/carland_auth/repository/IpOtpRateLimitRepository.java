package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.IpOtpRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpOtpRateLimitRepository extends JpaRepository<IpOtpRateLimit, Long> {
    Optional<IpOtpRateLimit> findByIpAddress(String ipAddress);
}
