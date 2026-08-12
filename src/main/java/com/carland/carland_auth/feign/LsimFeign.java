package com.carland.carland_auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lsimClient", url = "${lsim.api.url}")
public interface LsimFeign {

    @GetMapping("/quicksms/v1/send")
    String sendSms(@RequestParam("login") String login,
                   @RequestParam("msisdn") String msisdn,
                   @RequestParam("text") String text,
                   @RequestParam("sender") String sender,
                   @RequestParam("key") String key,
                   @RequestParam(value = "unicode", required = false) boolean unicode);

    @GetMapping("/quicksms/v1/balance")
    String getBalance(@RequestParam("login") String login,
                      @RequestParam("key") String key);
}
