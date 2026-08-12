package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.SmsBalanceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsBalanceAlertRepository extends JpaRepository<SmsBalanceAlert, Integer> {
}
