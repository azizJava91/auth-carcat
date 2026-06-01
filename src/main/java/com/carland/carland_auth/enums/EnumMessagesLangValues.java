package com.carland.carland_auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnumMessagesLangValues {

    MISSING_BODY(
            "Məlumatlar əksikdir!",
            "Missing body!",
            "Данные отсутствуют!"
    ),

    MISSING_FIELDS(
            "Mobil nömrə, ad, soyad, doğum tarixi boş ola bilməz",
            "Mobile number, name, surname, and birth date cannot be empty",
            "Номер телефона, имя, фамилия и дата рождения не могут быть пустыми"
    ),
    USERNAME_ALREADY_EXISTS(
            "Bu nömrə ilə artıq qeydiyyat mövcuddur",
            "An account with this number already exists",
            "Аккаунт с этим номером уже существует"
    ),
    REGISTER_SUCCESS(
            "Qeydiyyat uğurludur",
            "Registration successful",
            "Регистрация прошла успешно"
    ),
    REGISTER_SUCCESS_UPDATED(
            "Qeydiyyat uğurludur (mövcud istifadəçi yeniləndi)",
            "Registration successful (existing user updated)",
            "Регистрация прошла успешно (существующий пользователь обновлён)"
    ),
    MISSING_USER_FIELDS(
            "İstifadəçi adı, ID və şifrə boş ola bilməz",
            "Username, ID and password cannot be empty",
            "Имя пользователя, ID и пароль не могут быть пустыми"
    ),
    USER_NOT_FOUND(
            "İstifadəçi mövcud deyil",
            "User not found",
            "Пользователь не найден"
    ),
    WRONG_PASSWORD(
            "Daxil etdiyiniz şifrə yanlışdır.",
            "The password you entered is incorrect",
            "Введённый пароль неверный."
    ),
    LOGIN_SUCCESS(
            "Uğurlu giriş",
            "Login successful",
            "Вход выполнен успешно"
    ),
    MISSING_USER_ID(
            "İstifadəçi ID boş ola bilməz",
            "User ID cannot be empty",
            "ID пользователя не может быть пустым"
    ),
    REFRESH_TOKEN_NOT_FOUND(
            "Token tapılmadı",
            "Token not found",
            "Токен не найден"
    ),
    REFRESH_TOKEN_SUCCESS(
            "Token uğurla yeniləndi",
            "Token refreshed successfully",
            "Токен успешно обновлён"
    ),
    INVALID_USER_STATUS(
            "İstifadəçi uyğun statusda deyil",
            "User is not in a valid status",
            "Пользователь имеет недопустимый статус"
    ),
    PASSWORD_SET_SUCCESS(
            "Şifrə təyin edildi",
            "Password set successfully",
            "Пароль успешно установлен"
    ),
    MISSING_PHONE_NUMBER(
            "Telefon nömrəsi boş ola bilməz",
            "Phone number cannot be empty",
            "Номер телефона не может быть пустым"
    ),
    OTP_SENT(
            "Otp göndərildi",
            "OTP sent",
            "OTP отправлен"
    ),

    CARLAND_SERVICE_ERROR(
            "Xəta baş verdi. Bir az sonra yenidən cəhd edin",
            "An error occurred. Please try again later",
            "Произошла ошибка. Попробуйте позже"
    ),

    INVALID_OTP_CODE(
            "OTP kod yanlışdır",
            "OTP code is incorrect",
            "Неверный код OTP"
    ),
    ACCESS_TOKEN_MISSING(
            "Access token göndərilmədi",
            "Access token is missing",
            "Access токен не был отправлен"
    ),


    REFRESH_TOKEN_MISSING(
            "Refresh token göndərilmədi",
            "Refresh token is missing",
            "Отсутствует refresh токен"
    ),

    REGISTER_TOKEN_EXPIRED(
            "Register tokenin vaxtı keçib",
            "Register token expired",
            "Срок действия регистрационного токена истек"
    ),
    EXPIRED_OTP(
            "OTP aktivlik müddəti bitmişdir",
            "The OTP validity period has expired.",
            "Срок активности OTP истёк"
    ),
    OTP_VERIFIED_SUCCESS(
            "Otp təstiqləmə uğurla başa çatdı",
            "OTP verification completed successfully",
            "Проверка OTP успешно завершена"
    ),
    SUCCESS(
            "Uğurla tamamlandı",
            "Success",
            "Успешно"
    ),
    OTP_NOT_FOUND(
            "Şərtlərə uyğun OTP tapılmadı",
            "OTP not found for the given conditions",
            "OTP не найдено для указанных условий"
    ),
    MSM_TRANSACTION_ERROR(
            "Xəta baş verdi. Bir az sonra yenidən cəhd edin",
            "An error occurred. Please try again later",
            "Произошла ошибка. Попробуйте позже"
    ),

    TOKEN_INVALID(
            "Token düzgün deyil və ya vaxtı keçib",
            "Token is invalid or expired",
            "Токен недействителен или истек"),

    TOKEN_MISSING(
            "Token yoxdur və ya düzgün deyil",
            "Token is missing or invalid",
            "Токен отсутствует или недействителен"),

    TOKEN_EXPIRED(
            "Token müddəti bitib",
            "Token expired",
            "Token expired"),
    USER_SUCCESSFULLY_DELETED(
            "İstifadəçi silindi",
            "User deleted",
            "Пользователь удалено"
    ),
    INVALID_ROLE_PERMISSION(
            "Əməliyyat üçün səlahiyyətiniz yoxdur!",
            "You dont have permission for operation!",
            "У вас нет прав для операции!"
    );


    private final String azMessage;
    private final String enMessage;
    private final String ruMessage;

    public String getMessageByLang(String lang) {
        if (lang == null) return azMessage;
        return switch (lang.toLowerCase()) {
            case "az" -> azMessage;
            case "en" -> enMessage;
            case "ru" -> ruMessage;
            default -> azMessage;
        };
    }
}
