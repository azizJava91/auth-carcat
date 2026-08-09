package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.ConsumedAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsumedAuthTokenRepository extends JpaRepository<ConsumedAuthToken, String> {
}
