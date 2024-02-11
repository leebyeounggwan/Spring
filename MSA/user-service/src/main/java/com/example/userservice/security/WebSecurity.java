package com.example.userservice.security;

import com.example.userservice.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class WebSecurity {
    private final CustomAuthenticationManager customAuthenticationManager;
    private final UserRepository userRepository;
    private final Environment environment;
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().
                requestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .requestMatchers(new AntPathRequestMatcher( "/favicon.ico"))
                .requestMatchers(new AntPathRequestMatcher( "/css/**"))
                .requestMatchers(new AntPathRequestMatcher( "/js/**"))
                .requestMatchers(new AntPathRequestMatcher( "/img/**"))
                .requestMatchers(new AntPathRequestMatcher( "/lib/**"))
                .requestMatchers(new AntPathRequestMatcher( "/actuator/**"));
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .headers(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(authorize ->
//                authorize.requestMatchers(new MvcRequestMatcher(introspector, "/**"))
//                        .authenticated())
                authorize.requestMatchers((new MvcRequestMatcher(introspector, "/**"))).permitAll()
                        .anyRequest()
                        .authenticated())
                        .addFilter(getAuthenticationFilter())
                        .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    private AuthenticationFilter getAuthenticationFilter() {
        return new AuthenticationFilter(customAuthenticationManager, userRepository, environment);
    }
}
