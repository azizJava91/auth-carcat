package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {


    RefreshToken findTopByUserIdOrderByCreatedAtDesc(Long userId);

}
