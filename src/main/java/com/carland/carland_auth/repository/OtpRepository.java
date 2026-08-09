package com.carland.carland_auth.repository;

import com.carland.carland_auth.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Otp findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<Otp> findAllByUserIdAndStatus(Long id, String name);

    Otp findTopByPhoneNumberAndStatusOrderByCreatedAtDesc(String phoneNumber, String status);

    List<Otp> findAllByPhoneNumberAndStatus(String phoneNumber, String status);
}
