package com.example.apigatewayservice.config;

import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;

//@Configuration
public class FilterConfig {
    // yml에서 설정한 라우팅 정보를 자바 코드로 작성
    //@Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/first-service/**")
                            .filters(f -> f.addRequestHeader("first-request", "first-request-header")
                                           .addResponseHeader("first-response", "first-response-header"))
                            .uri("http://localhost:8081/"))
                .route(r -> r.path("/second-service/**")
                            .filters(f -> f.addRequestHeader("second-request", "second-request-header")
                                           .addResponseHeader("second-response", "second-response-header"))
                            .uri("http://localhost:8082/"))
                .build();
    }
}
