package com.carland.carland_auth.service.impl;


import com.carland.carland_auth.dto.request.InviteRequest;
import com.carland.carland_auth.dto.request.UserRequest;
import com.carland.carland_auth.dto.response.InviteResponse;
import com.carland.carland_auth.dto.response.AuthenticationResponse;
import com.carland.carland_auth.dto.response.UserResponse;
import com.carland.carland_auth.entity.RefreshToken;
import com.carland.carland_auth.entity.User;
import com.carland.carland_auth.enums.EnumMessagesLangValues;
import com.carland.carland_auth.enums.UserRoles;
import com.carland.carland_auth.enums.UserStatus;
import com.carland.carland_auth.exceptions.*;
import com.carland.carland_auth.feign.CarlandFeign;
import com.carland.carland_auth.jwt.JWTService;
import com.carland.carland_auth.repository.RefreshTokenRepository;
import com.carland.carland_auth.repository.UserRepository;
import com.carland.carland_auth.service.interfaces.OtpService;
import com.carland.carland_auth.service.interfaces.RefreshTokenService;
import com.carland.carland_auth.service.interfaces.UserService;
import com.carland.carland_auth.util.PinValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${access.token.expiration}")
    private Long ACCESS_TOKEN_EXPIRATION;

    @Value("${authentication.token.expiration}")
    private Long AUTHENTICATION_TOKEN_EXPIRATION;

    @Value("${pin.max-attempts:3}")
    private int pinMaxAttempts;

    @Value("${pin.attempt-window-minutes:10}")
    private int pinAttemptWindowMinutes;

    @Value("${pin.lock-duration-minutes:5}")
    private int pinLockDurationMinutes;


    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final CarlandFeign carlandFeign;

    @Transactional
    @Override
    public AuthenticationResponse authenticate(UserRequest request, String role, String acceptLanguage) {
        if (request == null || role == null) {
            log.info("missing body error");
            throw new HttpMessageConversionException(EnumMessagesLangValues.MISSING_BODY
                    .getMessageByLang(acceptLanguage));
        }
        log.info("request from controller for authenticate : {}", request);

        if (request.getPhoneNumber() == null) {
            log.info("missing field error");
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }


        User existingUser = userRepository.findByPhoneNumber(request.getPhoneNumber());
        if (existingUser != null) {
            if (UserStatus.ACTIVE.name().equalsIgnoreCase(existingUser.getStatus())) {
                log.info("user already exists");
                throw new UsernameAlreadyExistException(EnumMessagesLangValues.USERNAME_ALREADY_EXISTS
                        .getMessageByLang(acceptLanguage));
            }

            existingUser.setCreatedAt(LocalDateTime.now());
            existingUser.setStatus(UserStatus.OTP_PENDING.name());
            existingUser.setRole(role.toUpperCase());
            userRepository.save(existingUser);
            log.info("updated existing user: {}", existingUser);
            String authenticationJwt = jwtService.generateAuthenticationToken(existingUser, AUTHENTICATION_TOKEN_EXPIRATION);
            return AuthenticationResponse.ofToken(
                    authenticationJwt,
                    EnumMessagesLangValues.REGISTER_SUCCESS_UPDATED.getMessageByLang(acceptLanguage));
        }
        User newUser = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .pin(UUID.randomUUID().toString())
                .name(request.getName())
                .surname(request.getSurname())
                .createdAt(LocalDateTime.now())
                .role(role.toUpperCase())
                .status(UserStatus.OTP_PENDING.name())
                .build();
        userRepository.save(newUser);
        log.info("new user {}", newUser);
        String authenticationJwt = jwtService.generateAuthenticationToken(newUser, AUTHENTICATION_TOKEN_EXPIRATION);
        return AuthenticationResponse.ofToken(
                authenticationJwt,
                EnumMessagesLangValues.REGISTER_SUCCESS.getMessageByLang(acceptLanguage));
    }


    @Override
    @Transactional
    public UserResponse login(UserRequest request, String acceptLanguage) {
        if (request == null) {
            log.info("missing body error");
            throw new HttpMessageConversionException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        log.info("request from controller for login : {}", request);
        String credential = request.resolveCredential();
        if (request.getPhoneNumber() == null || credential == null) {
            log.info("missing field error");
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_USER_FIELDS.getMessageByLang(acceptLanguage));
        }

        User user = userRepository.findByPhoneNumberAndStatus(request.getPhoneNumber(), UserStatus.ACTIVE.name());
        log.info("user from repository  : {}", user);
        if (user == null) {
            log.info("user not found error");
            throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        LocalDateTime now = LocalDateTime.now();
        clearExpiredPinLock(user, now);

        if (isPinLocked(user, now)) {
            throwPinLocked(user, acceptLanguage, now);
        }

        if (!passwordEncoder.matches(credential, user.getPin())) {
            handleWrongPin(user, acceptLanguage, now);
        }

        resetPinFailureState(user);

        String accessTokenJWT = jwtService.generateAccessToken(user, ACCESS_TOKEN_EXPIRATION);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user, request.getDeviceToken(), request.getPlatform());

        List<RefreshToken> refreshTokenList = user.getRefreshTokens();
        refreshTokenList.add(refreshToken);

        refreshToken.setUser(user);
        userRepository.save(user);

        return UserResponse.builder()
                .accessToken(accessTokenJWT)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole())
                .userId(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .phoneNumber(user.getPhoneNumber())
                .message(EnumMessagesLangValues.LOGIN_SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Override
    public UserResponse refresh(Long userId, String refreshToken, String acceptLanguage) {

        if (userId == null) {
            log.info("missing  field error ");
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_USER_ID.getMessageByLang(acceptLanguage));
        }
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE.name());
        if (user == null) {
            log.info("user not found error");
            throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        RefreshToken refreshTokenEntity = refreshTokenRepository.findTopByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId);
        if (refreshTokenEntity == null) {
            refreshTokenEntity = refreshTokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        }

        log.info("refresh token from repository : {}", refreshTokenEntity);

        if (refreshTokenEntity == null || refreshTokenEntity.getRevokedAt() != null) {
            throw new RefreshTokenNotSetException(EnumMessagesLangValues.REFRESH_TOKEN_NOT_FOUND
                    .getMessageByLang(acceptLanguage));
        }

        String raw = refreshToken != null && refreshToken.startsWith("Bearer ")
                ? refreshToken.substring(7)
                : refreshToken;
        if (!refreshTokenEntity.getToken().equals(raw)) {
            throw new RefreshTokenNotSetException(EnumMessagesLangValues.REFRESH_TOKEN_NOT_FOUND
                    .getMessageByLang(acceptLanguage));
        }

        String accessTokenJWT = jwtService.generateAccessToken(user, ACCESS_TOKEN_EXPIRATION);


        return UserResponse.builder()
                .refreshToken(refreshTokenEntity.getToken())
                .accessToken(accessTokenJWT)
                .role(user.getRole())
                .name(user.getName())
                .surname(user.getSurname())
                .phoneNumber(user.getPhoneNumber())
                .message(EnumMessagesLangValues.REFRESH_TOKEN_SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Override
    @Transactional
    public UserResponse setPin(UserRequest userRequest, Long userId, String acceptLanguage) {

        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage)));

        if (!user.getStatus().equals(UserStatus.OTP_VERIFIED.name())) {
            throw new InvalidStatusException(EnumMessagesLangValues.INVALID_USER_STATUS.getMessageByLang(acceptLanguage));
        }
        String credential = userRequest.resolveCredential();
        PinValidator.validate(credential, acceptLanguage);
        String hashedPin = passwordEncoder.encode(credential);
        user.setPin(hashedPin);
        user.setStatus(UserStatus.ACTIVE.name());

        return UserResponse.builder()
                .message(EnumMessagesLangValues.PASSWORD_SET_SUCCESS.getMessageByLang(acceptLanguage))
                .role(user.getRole())
                .name(user.getName())
                .surname(user.getSurname())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    @Override
    public AuthenticationResponse updatePin(UserRequest userRequest, String acceptLanguage) {
        if (userRequest.getPhoneNumber() == null) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_PHONE_NUMBER.getMessageByLang(acceptLanguage));
        }

        User user = userRepository.findByPhoneNumber(userRequest.getPhoneNumber());

        if (user == null || user.getStatus().equalsIgnoreCase(UserStatus.DELETED.name())) {
            throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        otpService.createOtp(user.getId(), acceptLanguage);
        String authenticationJwt = jwtService.generateAuthenticationToken(user, AUTHENTICATION_TOKEN_EXPIRATION);
        return AuthenticationResponse.ofToken(
                authenticationJwt,
                EnumMessagesLangValues.OTP_SENT.getMessageByLang(acceptLanguage));
    }

    @Override
    @Transactional
    public InviteResponse inviteUser(Long inviterId, String inviterRole, InviteRequest inviteRequest,
                                     String acceptLanguage) {
        if (!(inviterRole.equals(UserRoles.BOSS.name()) || inviterRole.equals(UserRoles.SUPER_ADMIN.name()))) {
            throw new InvalidStatusException(EnumMessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }
        User inviterUser = userRepository.findByIdAndStatus(inviterId, UserStatus.ACTIVE.name());

        if (inviterUser == null) {
            throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        User existingUser = userRepository.findByPhoneNumber(inviteRequest.getPhoneNumber());
        if (existingUser != null) {
            throw new UsernameAlreadyExistException(EnumMessagesLangValues.USERNAME_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }

        String newUserRole = inviterUser.getRole().equals(UserRoles.BOSS.name())
                ? UserRoles.SUPER_ADMIN.name()
                : UserRoles.ADMIN.name();

        String credential = inviteRequest.resolveCredential();
        PinValidator.validate(credential, acceptLanguage);

        User newUser = User.builder()
                .name(inviteRequest.getName())
                .surname(inviteRequest.getSurname())
                .pin(passwordEncoder.encode(credential))
                .phoneNumber(inviteRequest.getPhoneNumber())
                .createdAt(LocalDateTime.now())
                .role(newUserRole)
                .status(UserStatus.ACTIVE.name())
                .build();
        userRepository.save(newUser);

        String token = jwtService.generateAccessToken(newUser, ACCESS_TOKEN_EXPIRATION);

        UserResponse userResponse = carlandFeign.addUserDetails("Bearer " + token, newUser.getRole(),
                newUser.getPhoneNumber(), newUser.getName(), newUser.getSurname(), newUser.getId().toString(),
                "Asia/Baku", "az", inviterId);

        if (userResponse == null || !userResponse.getMessage().equals(EnumMessagesLangValues.SUCCESS.getMessageByLang("az"))) {
            throw new InvalidStatusException(EnumMessagesLangValues.CARLAND_SERVICE_ERROR.getMessageByLang(acceptLanguage));
        }

        return InviteResponse.builder()
                .message(EnumMessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Override
    public UserResponse deleteUser(Long userId, String acceptLanguage) {

        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage)));

        user.setStatus(UserStatus.DELETED.name());
        userRepository.save(user);
        return UserResponse.builder()
                .message(EnumMessagesLangValues.USER_SUCCESSFULLY_DELETED.getMessageByLang(acceptLanguage))
                .build();
    }

    private void clearExpiredPinLock(User user, LocalDateTime now) {
        if (user.getPinLockedUntil() != null && !user.getPinLockedUntil().isAfter(now)) {
            user.setPinLockedUntil(null);
        }
    }

    private boolean isPinLocked(User user, LocalDateTime now) {
        return user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(now);
    }

    private void throwPinLocked(User user, String acceptLanguage, LocalDateTime now) {
        long remainingSeconds = Math.max(1, java.time.Duration.between(now, user.getPinLockedUntil()).getSeconds());
        throw new PinLockedException(
                EnumMessagesLangValues.PIN_LOCKED.getMessageByLang(acceptLanguage),
                user.getPinLockedUntil(),
                remainingSeconds
        );
    }

    private void handleWrongPin(User user, String acceptLanguage, LocalDateTime now) {
        log.info("wrong pin error userId={}", user.getId());

        int attempts;
        if (user.getLastFailedPinAt() == null
                || user.getLastFailedPinAt().isBefore(now.minusMinutes(pinAttemptWindowMinutes))) {
            attempts = 1;
        } else {
            int current = user.getFailedPinAttempts() == null ? 0 : user.getFailedPinAttempts();
            attempts = current + 1;
        }

        user.setFailedPinAttempts(attempts);
        user.setLastFailedPinAt(now);

        if (attempts >= pinMaxAttempts) {
            user.setPinLockedUntil(now.plusMinutes(pinLockDurationMinutes));
            user.setFailedPinAttempts(0);
            userRepository.save(user);
            throwPinLocked(user, acceptLanguage, now);
        }

        userRepository.save(user);
        throw new WrongPasswordException(EnumMessagesLangValues.WRONG_PASSWORD.getMessageByLang(acceptLanguage));
    }

    private void resetPinFailureState(User user) {
        user.setFailedPinAttempts(0);
        user.setLastFailedPinAt(null);
        user.setPinLockedUntil(null);
    }

}
