# MSA

# Eureka Server

MSA에서 서비스 간의 통신과 발견을 관리하는데 사용 

- 각각의 마이크로 서비스는 Eureka 클라이언트를 사용하여 서비스를 등록하고 메타데이터를 제공한다. 다른 서비스가 필요할 때 Eureka Server를 통해 서비스를 찾을 수 있다.

1. UserService 생성
2. Eureka 클라이언트 등록 후 실행하면 dashboard에 정상적으로 나타난다.
3. 같은 service를 하나 더 실행 → edit configuration → vm option → `-Dserver.port=9002`
4. cmd
    
    `./gradlew build` 빌드
    
    `./gradlew bootRun --args='--server.port=9003’` 빌드 시 포트지정
    
    `java -jar build/libs/your-application.jar` 실행
    
    `java -jar -Dserver.port=9004 ./target/user-service-0.0.1-SNAPSHOT.jar` 실행 시 포트지정
    

여러개의 서비스를 시작하는 방법인데 귀찮다.

스프링부트에서 제공하는 랜덤포트를 사용하면 간단히 여러 개를 띄울 수 있다.

application.yml 에서 `server: port: 0` 으로 지정하면 랜덤포트로 할당된다.

랜덤포트로 2개의 인스턴스를 기동했지만 Eureka dashboard에는 0번포트에 1개만 확인된다.

(실제 port가 아닌 yml파일에 설정된 0번을 기준으로 표시가 된다.)

→ yml/eureka에 instance-id를 추가

`eureka:  instance:    instance-id: ${spring.cloud.client.hostname}:${spring.application.instance_id:${random.value}} *# hostname + port*`

# API Gateway Service

사용자나 외부시스템으로 부터 요청을 단일화 

- 사용자가 설정한 라우팅 정보를 바탕으로 요청을 서비스로 보내고 응답을 전달하는 프록시 역할시스템의 내부구조는 숨기고 외부의 요청에 대해서 적절한 형태로 가공해서 응답할 수 있다.

클라이언트사이드에서 앤드포인트를 직접 호출 할 경우 서비스가 변경되면 클라이언트 사이드에 있는 어플리케이션도 수정이 필요하다

→ 단일 진입점의 필요성이 생김

인증 및 권한, 서비스 검색 통합, 응답캐싱, 부하 분산, 로깅 등 다양한 역할을 수행한다.

---

<aside>
💡 Spring Cloud에서 MSA간 통신
1) RestTemplate

2) Feign Client
@FeignCliden(”stores”) 서비스 이름 등록 후 이름으로 사용가능

</aside>

1. 라우팅 정보 등록

: 사용자의 요청에 대해 라우팅정보를 설정

**application.yml**

```yaml
spring:
  application:
    name: apigateway-service
  cloud:
    gateway:
      mvc:
        routes:
          - id: first-service
            uri: http://localhost:8001/  # 요청(predicates)이 들어오면 이 uri로 보내라
            predicates:                  # 조건(사용자가 요청한 url)
              - Path=/first-service/**
						filters: # 요청에 대한 처리
              - AddRequestHeader=first-service-header, first-service-header2 # 요청에 헤더 추가
              - AddResponseHeader=first-service-response-header, first-service-response-header2 # 응답에 헤더 추가
          - id: second-service
            uri: http://localhost:8002/
            predicates:
              - Path=/second-service/**
						filters:
                - AddRequestHeader=second-service-header, second-service-header2
                - AddResponseHeader=second-service-response-header, second-service-response-header2
```

** Tomcat이 아닌 netty서버(비동기 방식의 내장서버)가 기동된다.

- 어플리케이션 정보를 yml이 아닌 filter를 만들어 등록

```java
@Configuration
public class FilterConfig {
    
    @Bean
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
```

# Filter

: 로그, 인증 등 필터를 통해 작업 가능

### 1. CustomFilter

- AbstractGatewayFilterFactory<CustomFilter.Config>를 상속받아 Custom Filter 정의 가능 
필요한 기능은 apply 메서드를 상속받아 기능을 정의하고 GatewayFilter를 return

```java
@Component
@Slf4j
public class CustomFilter extends AbstractGatewayFilterFactory<CustomFilter.Config> {
    public CustomFilter() {
        super(Config.class);
    }

    public static class Config {

    }

    @Override
    public GatewayFilter apply(Config config) {
        // Custom Pre Filter
        return ((exchange, chain) -> { // exchange: 요청, 응답 정보를 가지고 있음 / chain: 다음 필터를 연결하는 역할
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Custom PRE filter: request id -> {}", request.getId());

            // Custom Post Filter
            return chain.filter(exchange).then(Mono.fromRunnable(() -> { // Mono: 비동기적으로 작업을 수행할 수 있도록 도와주는 객체
                log.info("Custom POST filter: response code -> {}", response.getStatusCode());
            }));
        });
    }
}
```

** ServerHttpRequest → 톰캣에서 사용하던 ServletReq,Res가 아닌 비동기 방식에서는 ServerReq,Res를 사용한다. `Fegin Client, WebFlux, Mono`

### 2. Global Filter

: 모든 요청에 적용되는 필터

<aside>
💡  .yml에서 `routes`에 개별 적용했던 Custom Filter와 달리 `cloud/gateway/default-filters`를 등록하고 전달하고자 하는 파라미터를 지정할 수 있다.

</aside>

- GlobalFilter는 가장 처음 실행되고 가장 마지막에 종료된다.

```yaml
spring:
  application:
    name: apigateway-service
  cloud:
    gateway:
      default-filters:
				# Global Filter 추가
        - name: GlobalFilter
          args:
            baseMessage: Spring Cloud Gateway Default Filter
            preLogger: true
            postLogger: true
      routes:
          - id: first-service
            uri: http://localhost:8001/ 
            predicates: 
              - Path=/first-service/**
            filters: 
              - CustomFilter
          - id: second-service
            uri: http://localhost:8002/
            predicates:
              - Path=/second-service/**
            filters:
               - CustomFilter
```

```java
@Component
@Slf4j
public class GlobalFilter extends AbstractGatewayFilterFactory<GlobalFilter.Config> {
    public GlobalFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {

        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Global filter baseMessage: {}", config.getBaseMessage());

            if (config.isPreLogger()) {
                log.info("Global filter Start: request id -> {}", request.getId());
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if (config.isPostLogger()) {
                    log.info("Global filter End: response code -> {}", response.getStatusCode());
                }
            }));
        });
    }

    @Data
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
```

### Loggin Filter

```yaml
# Second Service에만 LogginFliter 추가
- id: second-service
            uri: http://localhost:8002/
            predicates:
              - Path=/second-service/**
            filters:
#                - AddRequestHeader=second-service-header, second-service-header2
#                - AddResponseHeader=second-service-response-header, second-service-response-header2
               - name: CustomFilter
               - name: LoggingFilter
                 args:
                   baseMessage: Hi, there.
                   preLogger: true
                   postLogger: true
```

```java
// 기존 lamda식과 동일한 기능
@Component
@Slf4j
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {
    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        GatewayFilter filter = new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Logging filter baseMessage: {}", config.getBaseMessage());

            if (config.isPreLogger()) {
                log.info("Logging PRE filter: request id -> {}", request.getId());
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if (config.isPostLogger()) {
                    log.info("Logging POST filter: response code -> {}", response.getStatusCode());
                }
            }));
        }, Ordered.HIGHEST_PRECEDENCE); // 필터의 우선순위를 지정
        return filter;
    }

    @Data
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
```

# Gateway - Eureka 연동

`요청 → API Gateway → Eureka Server → ApiGaeway → First Service`

1) apigateway에 Eureka Client dependency 설정 /  .yml 추가

- 전체 애플리케이션 개요
    - Catalog-Service : 상품조회
    - User-Service : 사용자조회, 주문 확인
    - Order-Service : 상품 주문, 주문 조회, 수량 업데이트

- Spring Security
    - AuthenticationFilter
        - attemptAuthentication()
            - UsernamePasswordAuthenticationToken
                - UserDetailService
                    - loadUserByUsername() → UserRepository.findByEmail()
        - successfulAuthentication()

# Spring Cloud Config
