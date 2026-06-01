package com.carland.carland_auth.controller;

import com.carland.carland_auth.dto.request.OtpRequest;
import com.carland.carland_auth.dto.response.OtpResponse;
import com.carland.carland_auth.enums.EnumMessagesLangValues;
import com.carland.carland_auth.jwt.JWTService;
import com.carland.carland_auth.service.interfaces.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor

public class OtpController {
    private final OtpService otpService;
    private final JWTService jwtService;

    @PostMapping("/createAndSend")
    public OtpResponse createOtp(@RequestHeader("Authorization") String registerToken,
                                 @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage) {
        Long userId = jwtService.extractUserIdFromRegisterToken(registerToken);
        return otpService.createOtp(userId, acceptLanguage);
    }


    @PostMapping("/verify")
    public OtpResponse verifyOtp(@RequestBody OtpRequest otpVerifyRequest,
                                 @RequestHeader("Authorization") String registerToken,
                                 @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage) {

        Long userId = jwtService.extractUserIdFromRegisterToken(registerToken);
        return otpService.verifyOtp(otpVerifyRequest, userId, acceptLanguage);
    }

}