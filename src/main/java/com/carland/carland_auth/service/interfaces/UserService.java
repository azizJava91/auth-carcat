package com.carland.carland_auth.service.interfaces;


import com.carland.carland_auth.dto.request.InviteRequest;
import com.carland.carland_auth.dto.request.UserRequest;
import com.carland.carland_auth.dto.response.InviteResponse;
import com.carland.carland_auth.dto.response.RegisterResponse;
import com.carland.carland_auth.dto.response.UserResponse;

public interface UserService {
    RegisterResponse register(UserRequest request, String role, String acceptLanguage);

    UserResponse login(UserRequest request, String acceptLanguage);

    UserResponse refresh(Long userId, String refreshToken, String acceptLanguage);

    UserResponse setPassword(UserRequest userRequest, Long userId, String acceptLanguage);

    RegisterResponse updatePassword(UserRequest userRequest, String acceptLanguage);

    InviteResponse inviteUser(Long inviterId, String inviterRole, InviteRequest inviteRequest, String acceptLanguage);

    UserResponse deleteUser(Long userId, String acceptLanguage);
}
