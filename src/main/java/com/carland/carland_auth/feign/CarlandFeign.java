package com.carland.carland_auth.feign;

import com.carland.carland_auth.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "carlandClient", url = "${carland.api.url}")

public interface CarlandFeign {

    @PostMapping("api/v1/user/add-details")
    UserResponse addUserDetails(@RequestHeader("Authorization") String token,
                                @RequestHeader("role") String role,
                                @RequestHeader("phoneNumber") String phoneNumber,
                                @RequestHeader("name") String name,
                                @RequestHeader("surname") String surname,
                                @RequestHeader("X-User-Id") String userIdHeader,
                                @RequestHeader("X-Client-Timezone") String timezone,
                                @RequestHeader("Accept-Language") String acceptLanguage,
                                @RequestHeader("inviterId") Long inviterId);
}
