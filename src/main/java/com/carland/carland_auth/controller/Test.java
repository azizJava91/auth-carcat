package com.carland.carland_auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invite-ui")
public class Test {
    @GetMapping("/get")
    public String test (){
        return "AAAAAAAASSSSSSSSSDDDDDDDFFFFFFFFGGGGGG";
    }
}
