package com.carland.carland_auth.new_users_version.service;

import com.carland.carland_auth.dto.request.UserRequest;
import com.carland.carland_auth.dto.response.UserResponse;
import com.carland.carland_auth.entity.*;
import com.carland.carland_auth.enums.EnumMessagesLangValues;
import com.carland.carland_auth.enums.OtpStatus;
import com.carland.carland_auth.enums.UserRoles;
import com.carland.carland_auth.enums.UserStatus;
import com.carland.carland_auth.exceptions.*;
import com.carland.carland_auth.jwt.JWTService;
import com.carland.carland_auth.new_users_version.dto.AuthFlowResponse;
import com.carland.carland_auth.new_users_version.dto.NewOtpRequest;
import com.carland.carland_auth.repository.*;
import com.carland.carland_auth.service.interfaces.RefreshTokenService;
import com.carland.carland_auth.service.interfaces.SMSService;
import com.carland.carland_auth.util.HashUtil;
import com.carland.carland_auth.util.PinValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewUsersService {

    public static final String NEXT_SEND_OTP = "SEND_OTP";
    public static final String NEXT_PIN_CHECK = "PIN_CHECK";
    public static final String NEXT_SET_PIN = "SET_PIN";
    public static final String PURPOSE_REGISTER = "REGISTER";
    public static final String PURPOSE_RESET = "RESET";
    public static final String STAGE_SET_PIN = "SET_PIN";

    @Value("${authentication.token.expiration}")
    private Long authTokenExpiration;

    @Value("${access.token.expiration}")
    private Long accessTokenExpiration;

    @Value("${otp.expiration-minutes:3}")
    private long otpExpirationMinutes;

    @Value("${otp.new.resend-cooldown-seconds:30}")
    private long resendCooldownSeconds;

    @Value("${otp.new.max-sends-per-window:3}")
    private int maxSendsPerWindow;

    @Value("${otp.new.send-window-minutes:15}")
    private int sendWindowMinutes;

    @Value("${otp.new.phone-lock-minutes:5}")
    private int phoneLockMinutes;

    @Value("${otp.new.ip-lock-hours:24}")
    private int ipLockHours;

    @Value("${otp.new.max-verify-attempts:3}")
    private int maxVerifyAttempts;

    @Value("${otp.new.verify-lock-minutes:5}")
    private int verifyLockMinutes;

    @Value("${pin.max-attempts:3}")
    private int pinMaxAttempts;

    @Value("${pin.attempt-window-minutes:10}")
    private int pinAttemptWindowMinutes;

    @Value("${pin.lock-duration-minutes:5}")
    private int pinLockDurationMinutes;

    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final OtpRepository otpRepository;
    private final OtpRateLimitRepository otpRateLimitRepository;
    private final IpOtpRateLimitRepository ipOtpRateLimitRepository;
    private final ConsumedAuthTokenRepository consumedAuthTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final SMSService smsService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthFlowResponse auth(UserRequest request, String acceptLanguage) {
        if (request == null) {
            throw new HttpMessageConversionException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_PHONE_NUMBER.getMessageByLang(acceptLanguage));
        }

        String purpose = normalizePurpose(request.getPurpose());
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber());

        if (PURPOSE_RESET.equals(purpose)) {
            if (user == null || UserStatus.DELETED.name().equalsIgnoreCase(user.getStatus())) {
                throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            String token = jwtService.generatePhoneAuthToken(request.getPhoneNumber(), PURPOSE_RESET, authTokenExpiration);
            return AuthFlowResponse.builder()
                    .authToken(token)
                    .next(NEXT_SEND_OTP)
                    .purpose(PURPOSE_RESET)
                    .message(EnumMessagesLangValues.OTP_SENT.getMessageByLang(acceptLanguage))
                    .build();
        }

        if (user != null && UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            String token = jwtService.generatePhoneAuthToken(request.getPhoneNumber(), PURPOSE_REGISTER, authTokenExpiration);
            return AuthFlowResponse.builder()
                    .authToken(token)
                    .next(NEXT_PIN_CHECK)
                    .purpose(PURPOSE_REGISTER)
                    .message(EnumMessagesLangValues.LOGIN_SUCCESS.getMessageByLang(acceptLanguage))
                    .build();
        }

        String token = jwtService.generatePhoneAuthToken(request.getPhoneNumber(), PURPOSE_REGISTER, authTokenExpiration);
        return AuthFlowResponse.builder()
                .authToken(token)
                .next(NEXT_SEND_OTP)
                .purpose(PURPOSE_REGISTER)
                .message(EnumMessagesLangValues.REGISTER_SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Transactional
    public AuthFlowResponse createAndSendNew(NewOtpRequest request, HttpServletRequest httpRequest, String acceptLanguage) {
        String authToken = requireAuthToken(request == null ? null : request.getAuthToken(), acceptLanguage);
        assertNotConsumed(authToken, acceptLanguage);
        assertPhoneAuthValid(authToken, acceptLanguage);

        String phone = jwtService.extractPhoneFromAuthToken(authToken);
        String ip = resolveClientIp(httpRequest);
        LocalDateTime now = LocalDateTime.now();

        enforceSendLimits(phone, ip, now, acceptLanguage);

        String code = generateOtpCode();
        String codeHash = HashUtil.sha256Hex(code);

        Otp otp = Otp.builder()
                .code(codeHash)
                .hashed(true)
                .status(OtpStatus.PENDING.name())
                .createdAt(now)
                .phoneNumber(phone)
                .build();
        otpRepository.save(otp);

        smsService.sendOtpToPhone(phone, code, acceptLanguage);
        recordSuccessfulSend(phone, ip, now);

        return AuthFlowResponse.builder()
                .authToken(authToken)
                .next(NEXT_SEND_OTP)
                .purpose(jwtService.extractPurposeFromAuthToken(authToken))
                .message(EnumMessagesLangValues.OTP_SENT.getMessageByLang(acceptLanguage))
                .build();
    }

    @Transactional
    public AuthFlowResponse verifyNew(NewOtpRequest request, String acceptLanguage) {
        if (request == null) {
            throw new HttpMessageConversionException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        String authToken = requireAuthToken(request.getAuthToken(), acceptLanguage);
        assertNotConsumed(authToken, acceptLanguage);
        assertPhoneAuthValid(authToken, acceptLanguage);

        String otpCode = request.getOtp() != null ? request.getOtp() : request.getOtpCode();
        if (otpCode == null || otpCode.isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }

        String phone = jwtService.extractPhoneFromAuthToken(authToken);
        String purpose = jwtService.extractPurposeFromAuthToken(authToken);
        LocalDateTime now = LocalDateTime.now();

        OtpRateLimit rate = otpRateLimitRepository.findByPhoneNumber(phone).orElse(null);
        if (rate != null && rate.getVerifyLockedUntil() != null && rate.getVerifyLockedUntil().isAfter(now)) {
            throwLocked(rate.getVerifyLockedUntil(), acceptLanguage, now);
        }

        Otp otpLast = otpRepository.findTopByPhoneNumberAndStatusOrderByCreatedAtDesc(phone, OtpStatus.PENDING.name());
        if (otpLast == null) {
            throw new InvalidOtpCodeException(EnumMessagesLangValues.INVALID_OTP_CODE.getMessageByLang(acceptLanguage));
        }

        boolean match = Boolean.TRUE.equals(otpLast.getHashed())
                ? HashUtil.sha256Hex(otpCode).equals(otpLast.getCode())
                : otpCode.equals(otpLast.getCode());

        if (!match) {
            handleWrongOtpVerify(phone, now, acceptLanguage);
        }

        if (now.isAfter(otpLast.getCreatedAt().plusMinutes(otpExpirationMinutes))) {
            throw new ExpiredOtpException(EnumMessagesLangValues.EXPIRED_OTP.getMessageByLang(acceptLanguage));
        }

        User user = userRepository.findByPhoneNumber(phone);
        if (user == null) {
            user = User.builder()
                    .phoneNumber(phone)
                    .pin(UUID.randomUUID().toString())
                    .createdAt(now)
                    .role(UserRoles.USER.name())
                    .status(UserStatus.OTP_VERIFIED.name())
                    .build();
            userRepository.save(user);
        } else {
            if (UserStatus.DELETED.name().equalsIgnoreCase(user.getStatus())) {
                throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            user.setStatus(UserStatus.OTP_VERIFIED.name());
            userRepository.save(user);
        }

        List<Otp> pending = otpRepository.findAllByPhoneNumberAndStatus(phone, OtpStatus.PENDING.name());
        pending.forEach(o -> o.setStatus(o.getId().equals(otpLast.getId())
                ? OtpStatus.SUCCESS.name() : OtpStatus.FAIL.name()));
        otpRepository.saveAll(pending);

        clearOtpCounters(phone);

        // Issue stage token for setPinCode (single-use after set).
        String setPinToken = jwtService.generatePhoneAuthToken(phone, purpose + "|" + STAGE_SET_PIN, authTokenExpiration);

        return AuthFlowResponse.builder()
                .authToken(setPinToken)
                .next(NEXT_SET_PIN)
                .purpose(purpose)
                .message(EnumMessagesLangValues.OTP_VERIFIED_SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Transactional
    public UserResponse setPinCode(UserRequest request, String purposeParam, String acceptLanguage) {
        if (request == null) {
            throw new HttpMessageConversionException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        if (purposeParam == null || purposeParam.isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }
        String purpose = purposeParam.trim().toUpperCase();
        if (!PURPOSE_REGISTER.equals(purpose) && !PURPOSE_RESET.equals(purpose)) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }

        String authToken = requireAuthToken(request.getAuthToken(), acceptLanguage);
        assertNotConsumed(authToken, acceptLanguage);
        assertPhoneAuthValid(authToken, acceptLanguage);

        // Token must be the one issued after verifyNew (stage SET_PIN).
        String purposeRaw = jwtService.extractPurposeFromAuthToken(authToken);
        if (purposeRaw == null || !purposeRaw.contains(STAGE_SET_PIN)) {
            throw new InvalidStatusException(EnumMessagesLangValues.INVALID_USER_STATUS.getMessageByLang(acceptLanguage));
        }

        String pinCode = request.resolveCredential();
        PinValidator.validate(pinCode, acceptLanguage);

        String phone = jwtService.extractPhoneFromAuthToken(authToken);
        User user = userRepository.findByPhoneNumber(phone);
        if (user == null) {
            throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        user.setPin(passwordEncoder.encode(pinCode));
        user.setStatus(UserStatus.ACTIVE.name());
        user.setFailedPinAttempts(0);
        user.setLastFailedPinAt(null);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        if (PURPOSE_RESET.equals(purpose)) {
            refreshTokenRepository.revokeAllExceptDevice(user.getId(), request.getDeviceToken(), LocalDateTime.now());
        }

        consumeAuthToken(authToken);

        return UserResponse.builder()
                .message(EnumMessagesLangValues.PASSWORD_SET_SUCCESS.getMessageByLang(acceptLanguage))
                .role(user.getRole())
                .name(user.getName())
                .surname(user.getSurname())
                .phoneNumber(user.getPhoneNumber())
                .userId(user.getId())
                .build();
    }

    @Transactional
    public UserResponse loginNew(UserRequest request, String acceptLanguage) {
        if (request == null) {
            throw new HttpMessageConversionException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        String pinCode = request.resolveCredential();
        if (request.getPhoneNumber() == null || pinCode == null) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_USER_FIELDS.getMessageByLang(acceptLanguage));
        }
        if (request.getDeviceToken() == null || request.getDeviceToken().isBlank()
                || request.getPlatform() == null || request.getPlatform().isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.DEVICE_REQUIRED.getMessageByLang(acceptLanguage));
        }

        User user = userRepository.findByPhoneNumberAndStatus(request.getPhoneNumber(), UserStatus.ACTIVE.name());
        if (user == null) {
            throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getPinLockedUntil() != null && !user.getPinLockedUntil().isAfter(now)) {
            user.setPinLockedUntil(null);
        }
        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(now)) {
            throwLocked(user.getPinLockedUntil(), acceptLanguage, now);
        }

        if (!passwordEncoder.matches(pinCode, user.getPin())) {
            handleWrongPin(user, acceptLanguage, now);
        }

        user.setFailedPinAttempts(0);
        user.setLastFailedPinAt(null);
        user.setPinLockedUntil(null);

        String accessToken = jwtService.generateAccessToken(user, accessTokenExpiration);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user, request.getDeviceToken(), request.getPlatform());
        refreshToken.setUser(user);
        user.getRefreshTokens().add(refreshToken);
        userRepository.save(user);

        return UserResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .role(user.getRole())
                .userId(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .phoneNumber(user.getPhoneNumber())
                .message(EnumMessagesLangValues.LOGIN_SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    private void handleWrongPin(User user, String acceptLanguage, LocalDateTime now) {
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
            throwLocked(user.getPinLockedUntil(), acceptLanguage, now);
        }
        userRepository.save(user);
        throw new WrongPasswordException(EnumMessagesLangValues.WRONG_PASSWORD.getMessageByLang(acceptLanguage));
    }

    private void handleWrongOtpVerify(String phone, LocalDateTime now, String acceptLanguage) {
        OtpRateLimit rate = otpRateLimitRepository.findByPhoneNumber(phone)
                .orElseGet(() -> OtpRateLimit.builder().phoneNumber(phone).sendCount(0).verifyFailCount(0).build());
        int fails = rate.getVerifyFailCount() == null ? 0 : rate.getVerifyFailCount();
        fails++;
        rate.setVerifyFailCount(fails);
        if (fails >= maxVerifyAttempts) {
            rate.setVerifyLockedUntil(now.plusMinutes(verifyLockMinutes));
            rate.setVerifyFailCount(0);
            otpRateLimitRepository.save(rate);
            throwLocked(rate.getVerifyLockedUntil(), acceptLanguage, now);
        }
        otpRateLimitRepository.save(rate);
        throw new InvalidOtpCodeException(EnumMessagesLangValues.INVALID_OTP_CODE.getMessageByLang(acceptLanguage));
    }

    private void enforceSendLimits(String phone, String ip, LocalDateTime now, String acceptLanguage) {
        IpOtpRateLimit ipLimit = ipOtpRateLimitRepository.findByIpAddress(ip)
                .orElseGet(() -> IpOtpRateLimit.builder().ipAddress(ip).sendCount(0).build());
        if (ipLimit.getLockedUntil() != null && ipLimit.getLockedUntil().isAfter(now)) {
            throwLocked(ipLimit.getLockedUntil(), acceptLanguage, now);
        }

        OtpRateLimit phoneLimit = otpRateLimitRepository.findByPhoneNumber(phone)
                .orElseGet(() -> OtpRateLimit.builder().phoneNumber(phone).sendCount(0).verifyFailCount(0).build());

        if (phoneLimit.getPhoneLockedUntil() != null && phoneLimit.getPhoneLockedUntil().isAfter(now)) {
            throwLocked(phoneLimit.getPhoneLockedUntil(), acceptLanguage, now);
        }
        if (phoneLimit.getLastSentAt() != null
                && phoneLimit.getLastSentAt().isAfter(now.minusSeconds(resendCooldownSeconds))) {
            throw new PinLockedException(
                    EnumMessagesLangValues.OTP_RESEND_COOLDOWN.getMessageByLang(acceptLanguage),
                    phoneLimit.getLastSentAt().plusSeconds(resendCooldownSeconds),
                    Math.max(1, java.time.Duration.between(now, phoneLimit.getLastSentAt().plusSeconds(resendCooldownSeconds)).getSeconds())
            );
        }

        // Persist stubs so recordSuccessfulSend can update.
        otpRateLimitRepository.save(phoneLimit);
        ipOtpRateLimitRepository.save(ipLimit);

        if (phoneLimit.getSendWindowStart() != null
                && !phoneLimit.getSendWindowStart().isBefore(now.minusMinutes(sendWindowMinutes))
                && phoneLimit.getSendCount() != null
                && phoneLimit.getSendCount() >= maxSendsPerWindow) {
            phoneLimit.setPhoneLockedUntil(now.plusMinutes(phoneLockMinutes));
            otpRateLimitRepository.save(phoneLimit);
            throwLocked(phoneLimit.getPhoneLockedUntil(), acceptLanguage, now);
        }

        // IP abuse: same window thresholds → 24h lock
        if (ipLimit.getSendWindowStart() != null
                && !ipLimit.getSendWindowStart().isBefore(now.minusMinutes(sendWindowMinutes))
                && ipLimit.getSendCount() != null
                && ipLimit.getSendCount() >= maxSendsPerWindow * 5) {
            ipLimit.setLockedUntil(now.plusHours(ipLockHours));
            ipOtpRateLimitRepository.save(ipLimit);
            throwLocked(ipLimit.getLockedUntil(), acceptLanguage, now);
        }
    }

    private void recordSuccessfulSend(String phone, String ip, LocalDateTime now) {
        OtpRateLimit phoneLimit = otpRateLimitRepository.findByPhoneNumber(phone).orElseThrow();
        if (phoneLimit.getSendWindowStart() == null
                || phoneLimit.getSendWindowStart().isBefore(now.minusMinutes(sendWindowMinutes))) {
            phoneLimit.setSendWindowStart(now);
            phoneLimit.setSendCount(1);
        } else {
            phoneLimit.setSendCount((phoneLimit.getSendCount() == null ? 0 : phoneLimit.getSendCount()) + 1);
        }
        phoneLimit.setLastSentAt(now);
        phoneLimit.setLastIp(ip);
        otpRateLimitRepository.save(phoneLimit);

        IpOtpRateLimit ipLimit = ipOtpRateLimitRepository.findByIpAddress(ip).orElseThrow();
        if (ipLimit.getSendWindowStart() == null
                || ipLimit.getSendWindowStart().isBefore(now.minusMinutes(sendWindowMinutes))) {
            ipLimit.setSendWindowStart(now);
            ipLimit.setSendCount(1);
        } else {
            ipLimit.setSendCount((ipLimit.getSendCount() == null ? 0 : ipLimit.getSendCount()) + 1);
        }
        ipOtpRateLimitRepository.save(ipLimit);
    }

    private void clearOtpCounters(String phone) {
        otpRateLimitRepository.findByPhoneNumber(phone).ifPresent(rate -> {
            rate.setVerifyFailCount(0);
            rate.setVerifyLockedUntil(null);
            rate.setSendCount(0);
            rate.setSendWindowStart(null);
            rate.setPhoneLockedUntil(null);
            otpRateLimitRepository.save(rate);
        });
    }

    private void throwLocked(LocalDateTime until, String acceptLanguage, LocalDateTime now) {
        long remaining = Math.max(1, java.time.Duration.between(now, until).getSeconds());
        throw new PinLockedException(
                EnumMessagesLangValues.OTP_RATE_LIMITED.getMessageByLang(acceptLanguage),
                until,
                remaining
        );
    }

    private String requireAuthToken(String authToken, String acceptLanguage) {
        if (authToken == null || authToken.isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.AUTH_TOKEN_MISSING.getMessageByLang(acceptLanguage));
        }
        return authToken.trim();
    }

    private void assertPhoneAuthValid(String authToken, String acceptLanguage) {
        if (!jwtService.isPhoneAuthTokenValid(authToken)) {
            throw new MissingFieldException(EnumMessagesLangValues.AUTH_TOKEN_INVALID.getMessageByLang(acceptLanguage));
        }
    }

    private void assertNotConsumed(String authToken, String acceptLanguage) {
        String hash = HashUtil.sha256Hex(stripBearer(authToken));
        if (consumedAuthTokenRepository.existsById(hash)) {
            throw new MissingFieldException(EnumMessagesLangValues.AUTH_TOKEN_INVALID.getMessageByLang(acceptLanguage));
        }
    }

    private void consumeAuthToken(String authToken) {
        consumedAuthTokenRepository.save(ConsumedAuthToken.builder()
                .tokenHash(HashUtil.sha256Hex(stripBearer(authToken)))
                .consumedAt(LocalDateTime.now())
                .build());
    }

    private static String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim();
        return t.regionMatches(true, 0, "Bearer ", 0, 7) ? t.substring(7).trim() : t;
    }

    private static String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return PURPOSE_REGISTER;
        }
        return purpose.trim().toUpperCase();
    }

    private static String generateOtpCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
