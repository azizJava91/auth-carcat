package com.carland.carland_auth.new_users_version.controller;

import com.carland.carland_auth.dto.request.UserRequest;
import com.carland.carland_auth.dto.response.UserResponse;
import com.carland.carland_auth.new_users_version.dto.AuthFlowResponse;
import com.carland.carland_auth.new_users_version.dto.NewOtpRequest;
import com.carland.carland_auth.new_users_version.service.NewUsersService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Parallel new auth API (PO flow). Base path avoids clash with legacy {@code /api/v1/users}.
 */
@RestController
@RequestMapping("/api/v1/newUsers")
@RequiredArgsConstructor
public class NewUsersController {

    private final NewUsersService newUsersService;

    @PostMapping("/auth")
    public AuthFlowResponse auth(@RequestBody UserRequest request,
                                 @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.auth(request, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PostMapping("/otp/createAndSendNew")
    public AuthFlowResponse createAndSendNew(@RequestBody NewOtpRequest request,
                                             HttpServletRequest httpRequest,
                                             @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.createAndSendNew(request, httpRequest, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PostMapping("/otp/verifyNew")
    public AuthFlowResponse verifyNew(@RequestBody NewOtpRequest request,
                                      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.verifyNew(request, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PutMapping("/setPinCode")
    public UserResponse setPinCode(@RequestBody UserRequest request,
                                   @RequestParam String purpose,
                                   @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.setPinCode(request, purpose, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PostMapping("/loginNew")
    public UserResponse loginNew(@RequestBody UserRequest request,
                                 @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.loginNew(request, acceptLanguage == null ? "az" : acceptLanguage);
    }
}
