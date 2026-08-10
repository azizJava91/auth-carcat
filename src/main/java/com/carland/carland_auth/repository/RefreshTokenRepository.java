package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    RefreshToken findTopByUserIdOrderByCreatedAtDesc(Long userId);

    RefreshToken findTopByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    // flushAutomatically: pending User.pinHash/status must hit DB before clearAutomatically drops the session
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :now where r.user.id = :userId and r.revokedAt is null "
            + "and (:keepDeviceId is null or r.deviceId is null or r.deviceId <> :keepDeviceId)")
    int revokeAllExceptDevice(@Param("userId") Long userId,
                              @Param("keepDeviceId") String keepDeviceId,
                              @Param("now") LocalDateTime now);
}
