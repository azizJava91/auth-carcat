package com.carland.carland_auth.service.interfaces;

import com.carland.carland_auth.dto.request.OtpRequest;
import com.carland.carland_auth.dto.response.OtpResponse;

public interface OtpService {
    OtpResponse createOtp(Long userId, String acceptLanguage);

    OtpResponse verifyOtp(OtpRequest otpRequest, Long userId, String acceptLanguage);

}
