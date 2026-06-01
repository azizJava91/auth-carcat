package com.carland.carland_auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "msmClient", url = "${msm.api.url}")
public interface MsmFeign {
    @GetMapping("/sendsms")
    String sendSms(@RequestParam("user") String username,
                        @RequestParam("password") String password,
                        @RequestParam("gsm") String number,
                        @RequestParam("from") String sender,
                        @RequestParam("text") String text);
}
