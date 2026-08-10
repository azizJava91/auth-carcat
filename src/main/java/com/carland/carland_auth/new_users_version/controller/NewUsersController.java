package com.carland.carland_auth.new_users_version.controller;

import com.carland.carland_auth.dto.request.UserRequest;
import com.carland.carland_auth.dto.response.UserResponse;
import com.carland.carland_auth.new_users_version.dto.AuthFlowResponse;
import com.carland.carland_auth.new_users_version.dto.NewOtpRequest;
import com.carland.carland_auth.new_users_version.dto.PinSetResponse;
import com.carland.carland_auth.new_users_version.service.NewUsersService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Parallel new auth API (PO). Base: {@code /api/v1/newUsers}.
 */
@RestController
@RequestMapping("/api/v1/newUsers")
@RequiredArgsConstructor
public class NewUsersController {

    private final NewUsersService newUsersService;

    @PostMapping("/auth")
    public AuthFlowResponse auth(@RequestBody UserRequest request,
                                 HttpServletRequest httpRequest,
                                 @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.auth(request, httpRequest, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PostMapping("/otp/createAndSend")
    public AuthFlowResponse createAndSend(@RequestBody NewOtpRequest request,
                                          HttpServletRequest httpRequest,
                                          @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.createAndSend(request, httpRequest, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PostMapping("/otp/verify")
    public AuthFlowResponse verify(@RequestBody NewOtpRequest request,
                                   @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.verify(request, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PutMapping("/setPinCode")
    public PinSetResponse setPinCode(@RequestBody UserRequest request,
                                     @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.setPinCode(request, acceptLanguage == null ? "az" : acceptLanguage);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody UserRequest request,
                              @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return newUsersService.login(request, acceptLanguage == null ? "az" : acceptLanguage);
    }
}
