package com.example.userservice.security;

import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userByEmail = userRepository.findUserByEmail(username);
        if (userByEmail == null) {
            throw new UsernameNotFoundException("User Not Found");
        }
        return new User(userByEmail.getEmail(),
                        userByEmail.getEncryptedPwd(),
                        true,
                        true,
                        true,
                        true,
                        new ArrayList<>());
    }
}
