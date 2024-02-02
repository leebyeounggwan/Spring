package com.example.userservice.security;

import com.example.userservice.jpa.UserEntity;
import com.example.userservice.jpa.UserRepository;
import com.example.userservice.vo.RequestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

@Slf4j
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final UserRepository userRepository;
    private final Environment environment;

    public AuthenticationFilter(AuthenticationManager authenticationManager, UserRepository userRepository, Environment environment) {
        this.userRepository = userRepository;
        this.environment = environment;
    }
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {

        try {
            RequestLogin creds = new ObjectMapper().readValue(request.getInputStream(), RequestLogin.class);
            System.out.println("creds.getEmail() = " + creds.getEmail());
            System.out.println("creds.getPwd() = " + creds.getPwd());
            return getAuthenticationManager() // getAuthenticationManager: 인증을 처리하는데 사용되는 AuthenticationManager 객체를 반환
                    .authenticate( // authenticate: 인증을 처리하는 메소드
                    new UsernamePasswordAuthenticationToken( // UsernamePasswordAuthenticationToken: 인증 요청에 사용되는 토큰
                            creds.getEmail(),
                            creds.getPwd(),
                            new ArrayList<>() // 인증된 사용자의 권한
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        String username = authResult.getName();
        UserEntity user = userRepository.findUserByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.debug("user id {}", user.getUserId());

        String token = Jwts.builder()
                .setSubject(user.getUserId())
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(Objects.requireNonNull(environment.getProperty("token.expiration.time")))))
                .signWith(SignatureAlgorithm.HS512, environment.getProperty("token.secret"))
                .compact();
        response.addHeader("token", token);
        response.addHeader("userId", user.getUserId());
    }
}
