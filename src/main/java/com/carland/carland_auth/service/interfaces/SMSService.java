package com.carland.carland_auth.service.interfaces;


public interface SMSService {
    void sendSms(Long userId, String acceptLanguage);
}
