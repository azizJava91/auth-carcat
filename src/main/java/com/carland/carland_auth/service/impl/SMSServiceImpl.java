package com.carland.carland_auth.service.impl;

import com.carland.carland_auth.entity.Otp;
import com.carland.carland_auth.entity.User;
import com.carland.carland_auth.enums.OtpStatus;
import com.carland.carland_auth.exceptions.*;
import com.carland.carland_auth.feign.LsimFeign;
import com.carland.carland_auth.repository.OtpRepository;
import com.carland.carland_auth.repository.UserRepository;
import com.carland.carland_auth.service.interfaces.SMSService;
import com.carland.carland_auth.util.Md5Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class SMSServiceImpl implements SMSService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final LsimFeign lsimFeign;

    @Value("${lsim.api.login}")
    private String login;

    @Value("${lsim.api.password}")
    private String password;

    @Value("${lsim.api.sender}")
    private String sender;

    @Value("${otp.expiration-minutes}")
    private long expirationMinutes;

    @Override
    public void sendSms(Long userId, String acceptLanguage) {

        if (userId == null) {
            throw new MissingFieldException("User id boşdur");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        String phone = user.getPhoneNumber();
        String number = phone != null ? phone.substring(1) : null;

        Otp otp = otpRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                userId, OtpStatus.PENDING.name());

        if (otp == null) {
            throw new ResourceNotFoundException("OTP not found");
        }

        if (LocalDateTime.now().isAfter(
                otp.getCreatedAt().plusMinutes(expirationMinutes))) {
            throw new ExpiredOtpException("OTP expired");
        }

        String message = "CarCat otp kodunuz: " + otp.getCode();


        String passMd5 = Md5Util.md5(password);
        String raw = passMd5 + login + message + number + sender;
        String key = Md5Util.md5(raw);

        log.info("OTP: {}", otp.getCode());

        String response = lsimFeign.sendSms(
                login,
                number,
                message,
                sender,
                key,
                true
        );

        log.info("LSIM response: {}", response);

        //   success check
//        if (response == null || response.toLowerCase().contains("error")) {
//            throw new RuntimeException("SMS gönderilmedi");
//        }
    }
}