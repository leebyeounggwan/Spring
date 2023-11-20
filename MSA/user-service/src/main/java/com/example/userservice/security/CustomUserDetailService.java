package com.example.userservice.security;

import com.example.userservice.dto.UserDto;
import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final UserService userFindPort;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //UserDetails userDetails = userFindPort.loadUserByUsername(username);
        UserEntity byEmail = userRepository.findByEmail(username);
        if (byEmail == null) {
            throw new UsernameNotFoundException(username);
        }
        UserDto userByEmail = new UserDto();
        userByEmail.setEmail(byEmail.getEmail());
        userByEmail.setPwd(byEmail.getEncryptedPwd());


        return new User(userByEmail.getEmail(), userByEmail.getPwd(), true, true, true, true, new ArrayList<>());
    }


}