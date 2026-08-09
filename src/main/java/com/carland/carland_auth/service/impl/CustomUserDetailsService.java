package com.carland.carland_auth.service.impl;

import com.carland.carland_auth.entity.User;
import com.carland.carland_auth.enums.EnumMessagesLangValues;
import com.carland.carland_auth.jwt.CustomUserDetails;
import com.carland.carland_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor

public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(username);

        if (user == null) {
            throw new UsernameNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang("az"));
        }

        return new CustomUserDetails(user.getPhoneNumber(), user.getPin());
    }
}
