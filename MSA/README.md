- 전체 애플리케이션 개요
    - ApiGateway-service : 라우팅, 로드밸런싱
    - Discovery-service : 마이크로서비스 등록, 메타데이터 제공
    - Config-service : 마이크로서비스 설정정보 등록
    - Catalog-Service : 상품조회
    - User-Service : 사용자 조회, 주문 확인
    - Order-Service : 상품 주문, 주문 조회, 수량 업데이트

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

# Spring Cloud Config

: 분산 시스템에서 서버 클라이언트 구성에 필요한 설정 정보(*.yml, *.properties)를 외부 시스템에서 관리 → 각 서비스를 다시 빌드하지 않고, 수정사항 적용 가능

Spring Cloud Config Server 

- 우선순위

`application.yml` → `application-name.yml` → `application-name-<profile>.yml`

```yaml
server:
  port: 8888

spring:
  application:
    name: config-service
  cloud:
    config:
      server:
        git:
          uri: file:///Users/Bottle_Coffin/Desktop/git-local-repo
          default-label: main
```

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

}
```

http://127.0.0.1:8888/ecommerce/default

![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/d9a7c5a5-25f7-431e-8232-b546062e54db/Untitled.png)

- 각각의 microservice에 bootsrap.yml을 등록하여 외부의 configuration 정보 파일을 등록

<aside>
💡 부트스트랩 컨텍스트는 애플리케이션의 주 설정 파일(**`application.yml`** 또는 **`application.properties`**)보다 먼저 외부 설정 소스에서 설정을 로드

</aside>

### ** Config Server에서 설정 정보가 변경 되었을 때 값을 가져오는 방법

1. 서버 재 기동 → config server를 사용하는 의미가 없다.
2. Actuator refresh → Actuator의 refresh 옵션을 사용
    
    > **Actuator** 
     - Application 상태, 모니터링
     - Metric 수집을 위한 Http End Point 제공
    > 
    - `actuator` dependency 추가
    - `GET` /   `localhost:10779/actuator/refresh`
    - `ecommerce.yml` 설정 정보 수정
    - postman → `POST` /  `localhost:10779/actuator/refresh`
    - `GET` /   `localhost:10779/health_check`
        
        → 변경된 설정 정보를 사용하는 서비스에 대해 각각 refresh 해야 한다.
        
3. Spring cloud bus → AMQP을 통해 각각의 MS에 변경사항을 갱신(Push)
    
    > Spring Cloud Bus
     - 분산 시스템의 노드(마이크로서비스)를 경량 메시지 브로커(Rabbit MQ)와 연결
     - 각각의 시스템 상태 및 구성에 대한 변경 사항을 연결된 노드에 전달
    > 
    
    <aside>
    💡 AMQP (Advanced Message Queuing Protocol)
    : 메시지 지향 미들웨어를 위한 개방형 표준 응용 계층 프로토콜
    
    - 비동기 메시지 전송 및 통신
    - 주로 메시지 브로커를 통해 메시지를 전달하고 관리
    - 분산 시스템 간의 효율적이고 안정적인 통신을 지원
    
    1. 메시지 큐: AMQP는 메시지 큐 시스템을 구축하고 관리하는 데 사용.
    메시지 큐는 메시지를 보내고 받는 시스템 간의 통신을 지원하며, 메시지의 안정적인 전송과 처리를 보장
    2. 프로듀서(Producer): 메시지를 생성하고 메시지 큐로 전송하는 역할을 하는 시스템 또는 응용 프로그램
    3. 컨슈머(Consumer): 메시지 큐에서 메시지를 소비하고 처리하는 역할을 하는 시스템 또는 응용 프로그램
    4. 브로커(Broker): 메시지의 전송 및 라우팅을 관리하는 중앙 시스템 또는 서버 AMQP 브로커는 메시지를 수신하고 발신하여 메시지 큐 간의 통신을 중계
    대표적인 AMQP 브로커로는 `RabbitMQ`와 `Apache ActiveMQ`
    5. 메시지 속성: AMQP 메시지는 헤더, 바디 및 속성과 같은 여러 부분으로 구성
    속성은 메시지에 대한 추가 정보를 제공하며, 메시지를 라우팅하고 필터링하는 데 사용
    6. 메시지 라우팅: AMQP는 메시지를 특정 큐로 라우팅하거나 토픽에 따라 메시지를 발행하고 구독할 수 있도록 지원. 메시지를 특정 컨슈머에게 전달하거나 여러 컨슈머에게 동시에 전달
    </aside>
    
    <aside>
    💡 Kafka
    : 오픈 소스 메시지 브로커 프로젝트
    
    - 분산형 스트리밍 플랫폼
    - 대용량 데이터 처리 가능한 메시징 시스템
    
    1. **분산 시스템**: Kafka는 여러 대의 브로커(서버)로 구성된 분산 시스템
    2. **토픽(Topic)**: 데이터 스트림을 정리하고 분류하기 위해 Kafka는 토픽을 사용
     토픽은 유사한 종류의 데이터를 그룹화하고 이를 여러 컨슈머에게 공급
    3. **프로듀서(Producer)**: 데이터를 Kafka 토픽으로 전송하는 역할을 하는 시스템 
    프로듀서는 데이터를 생성하고 Kafka 클러스터로 전송할 수 있습니다.
    4. **컨슈머(Consumer)**: Kafka에서 데이터를 소비하고 처리하는 역할을 하는 시스템  컨슈머는 특정 토픽에서 데이터를 읽고 처리
    5. **파티션(Partition)**: Kafka 토픽은 하나 이상의 파티션으로 나뉘어 진다.
     각 파티션은 독립적으로 데이터를 저장하고 컨슈머에게 분배 → 데이터의 병렬 처리와 확장성을 지원
    6. **레플리케이션(Replication)**: Kafka는 데이터의 내결함성을 보장하기 위해 파티션의 복제 기능. 각 파티션은 여러 브로커에 복제되어 장애 발생 시 데이터의 손실을 방지
    7. **로그(Log)**: Kafka는 데이터를 로그 형식으로 저장하며, 이를 통해 데이터의 보존과 검색 용이. 로그는 시간 순서대로 데이터를 저장하므로 실시간 데이터 스트림 처리에 적합
    </aside>
    
    - Actuator bus-refresh Endpoint
        
        : 분산 시스템의 노드를 경량 메시지 브로커(RabbitMQ/Kafaka 등)와 연결
        
        : 상태 및 구성에 대한 변경 사항을 연결된 노드에 Brodcast
        
        Spring Cloud Bus에 연결된 임의의 노드에 `HTTP POST /busrefresh` 를 호출하면 클라우드 버스에 변경사항을 알리고 연결된 노드 Update
        
    
    ## RabbitMQ
    
    1. RabbitMQ 설치 (ver 26.2.1)
    - Mac
        
        ```jsx
        $brew update
        $brew install rabitmq
        $export PATH=$PATH:/usr/local/sbin //환경변수 등록
        $rabbitmq-server OR ./rabbitmq-server // 실행
        
        -> 127.0.0.1:15672
        초기 로그인
        Username: [guest]
        Password: [guest]
        ```
        
    
    - Window
        
        : RabbitMQ는 Erlang이라는 언어로 개발되었기 때문에 사용하기 위해서는 Erlang이 설치되어 있어야 한다. Mac의 경우 brew에서 rabbitmq를 설치할 때 자동적으로 Erlang을 설치하지만 Window의 경우 별도로 Erlang을 설치해야한다.
        
        1. Erlang 설치
        2. RabbitMQ 설치
        설치 후 서비스에 RabbitMQ가 추가 됨
        3. Management Plugin 설치
            - powershell - `rabbitmq-plugins enable rabbitmq_management`
        4. rabbitmq-server 기동 후 → **127.0.0.1:15672**
        
    1. Dependencies 추가
        - Config Server
            
            Actuator, AMQP
            
        - User-service, Gatway-Service
            
            AMQP
            
        
    2. application.yml 수정
        
        Config / User / Gateway
        
        ```jsx
        // 각각의 서비스를 rabbitmq 노드로 등록
        spring:
        	rabbitmq:
        		host: 127.0.0.1
        		port: 5672 // webbrowser: 15672
        		username: guest
        		password: guest
        
        // actuator_Endpoint -> busrefresh 추가
        management:
        	endpoints:
        		web:
        			exposure:
        				include: refresh, health, beans, httptrace, **busrefresh**
        ```
        
    3. spring-cloud-config 의 application.yml 값 변경 `mySecret` → `mySecret_changed_#1`
        
        `127.0.0.1:8000/user-service/actuator/busrefresh` → 204 No Content
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/45b0f826-7d8d-483f-812f-c838aa5b5092/Untitled.png)
        
        apigateway-service
        
        : user-service를 통해 busrefresh를 호출하면 다른 서비스에서도 변경됨을 확인
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/3a581609-28d7-4b1a-a6b5-a15ac3428cb5/Untitled.png)
        
        - token의 key값이 변경되었기 때문에 기존 토큰으로는 인증불가
        - 다시 토큰을 발급받고 heath_check를 호출하면 yml파일에서 변경 한 key값 확인
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/a8ca8ad4-1a66-4f9e-9d84-b177a732fc65/Untitled.png)
        

# 암호화 처리

각각의 서비스에서 관리하던 application.yml파일의 database , RabbitMQ 등의 연결정보를 config-server로 이동하고 값을 노출하지 않기 위해 암호화 하여 저장

Encryption types

- 단방향
- 양방향 - 대칭/비대칭

JDK keytool

> JDK에서 제공되는 기능으로써 디지털 인증서, 키페어 및 보안 자격 증명을 관리하는데 사용
> 

- 대칭키 암호화 사용
    - config-service/bootstrap.yml → key값 지정 후 POST Body에 암호화 할 값 전달
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/9422592a-cf67-43bd-9059-52f86ffc1e93/Untitled.png)
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/b984d720-b1cd-4256-9d73-85e89dc13884/Untitled.png)
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/86f73bac-0359-4eed-a833-d23145ef3bea/Untitled.png)
    
    1. user-service/application.yml의 datasource를 spring-cloud-confg(git)으로 이동 및 주석
    2. user-service/bootstrap.yml의 name을 user-service로 변경
    3. datasource의 `password: 1234`를 `POST 127.0.0.1/8888/encrypt` 를 통해 암호화 하고 값을 user-service.yml의 password에 넣는다.
        
        암호화 된 값이라는 뜻으로 {cipher} 키워드를 추가 (명시하지 않으면 plain_text로 인식)
        
        ```jsx
        spring:
          datasource:
            url: jdbc:h2:tcp://localhost/~/testdb
            driver-class-name: org.h2.Driver
            username: sa
            password: '{cipher}cc0f049bfdd9ab8af544b747786b9969e74e687ccfec0635f558cddc0ae77bf9'
        
        token:
          expiration:
            time: 86400000
          secret: mySecret
        ```
        
        config-service를 통해 확인해보면 decrypt 된 값이 나온다.
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/0827f71f-e6c0-40ef-83b6-cfa373b25760/Untitled.png)
        
- 비대칭키 암호화 사용
    - jks 파일 생성
    
    ```jsx
    CMD
    
    // jks파일 생성
    keytool -genkeypair -alias apiEncryptionKey -keyalg RSA -dname "CN=Bg Lee, OU=API Development, O=localhost.co.kr, L=Seoul, C=KR" -keypass "test1234" -keystore apiEncryptionKey.jks -storepass "test1234"
    
    // 확인
    keytool -list -keystore apiEncryptionKey.jks -v
    
    // 인증서 내보내기 -> .cer 파일 생성
    keytool -export -alias apiEncryptionKey -keystore apiEncryptionKey.jks -rfc -file trustServer.cer
    
    // 인증서로 jks파일 생성
    keytool -import -alias trustServer -file trustServer.cer -keystore publicKey.jks
    ```
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/cb83ae86-c020-4034-bbc8-c89093e12aac/Untitled.png)
    
    - config-server에 jks파일 경로 지정
    
    ```jsx
    encrypt:
    #  key: abcdefghijklmnopqrstuvwxyz1234567890
      key-store:
        location: https://raw.githubusercontent.com/leebyeounggwan/spring-cloud-config/main/keystore/apiEncryptionKey.jks
        password: test1234
        alias: apiEncryptionKey
    ```
    
    - `POST 127.0.0.1:8888/encrypt` → 암호화 후 .yml에 {cipher}~~로 값 변경(이전과 동일)
    
    - `user-service/default`를 호출해도 공통적으로 application.yml을 갖고 있기 때문에 여러 서비스가 공유하는 설정정보의 경우 application.yml에 작성
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/75cddf8d-10f5-4a58-b881-9da74ff7413c/Untitled.png)
    

# 마이크로서비스간 통신

- Communication Types
    - 동기
    - 비동기(AMQP)

### 1. RestTemplate

<aside>
💡 **RestTemplate** 
: 클라이언트 측에서 서버로 HTTP 요청을 보낼 때 사용하는 Spring Framework의 클래스

**@RestController 
: 주로 서버 측에서 요청을 수신하고 응답을 생성하여 클라이언트에 반환하는 데 사용

>>>>> 
MA에서 사용하던 @RestController는 서버측에서 수신 및 응답을 위해 엔드포인트를 정의하고 요청을 처리하는 역할.

RestTemplate은 마이크로서비스에서 다른 서비스로 HTTP요청을 보내기 위해 사용하는 클래스.**

</aside>

> **User-Service → Order-Service**
UserService를 통해 User의 정보를 얻고 해당 정보로 Order Service를 호출하여 유저의 주문정보를 가져온다.
> 

1. UserServiceApplication에서 RestTemplate Bean 등록
    
    ```java
    @Bean
    public RestTemplate getRestTemplate() {
    		return new RestTemplate();
    }
    ```
    
2. user-service 수정
    - Enviroment, RestTemplate 생성자 주입
    - user-service.yml에 호출 할 order-service URL 정보 추가
    - [restTemplate.exchange](http://restTemplate.exchange) 메서드로 order-service 호출
    
    ```jsx
    // user-service
    @Override
        public UserDto getUserByUserId(String userId) {
            UserEntity userEntity = userRepository.findByUserId(userId);
    
            if(userEntity == null)
                throw new UsernameNotFoundException("User not found");
    
            UserDto userDto = new UserDto(userEntity);
    				
    
            //List<ResponseOrder> orders = new ArrayList<>();	기존에 작성했던 임시 주문정보 주석
    				
    				/* Using as rest template */
            String orderUrl = String.format(env.getProperty("order_service.url"), userId);
            ResponseEntity<List<ResponseOrder>> orderListResponse = 
                    restTemplate.exchange(orderUrl, HttpMethod.GET, null,
                                            new ParameterizedTypeReference<List<ResponseOrder>>() {
                    });
            
            List<ResponseOrder> orders = orderListResponse.getBody();
    
            userDto.setOrders(orders);
    
            return userDto;
        }
    ```
    
    `restTemplate.exchange(주소, Http메서드, 파라미터, 응답값)`
    
    - user-service에서 요청 할 order-service의 End-Point
    
    ```jsx
    // order-service
    @GetMapping("/{userId}/orders")
        public ResponseEntity<List<ResponseOrder>> getOrders(@PathVariable("userId") String userId) {
            Iterable<OrderEntity> orderList = orderService.getOrdersByUserId(userId);
    
            List<ResponseOrder> result = new ArrayList<>();
    
            orderList.forEach(v -> {
                result.add(new ResponseOrder(v));
            });
    
            return ResponseEntity.status(200).body(result);
        }
    ```
    
    - Postman 확인 결과
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/4c755c66-93fb-4261-ad2a-977720e26c07/Untitled.png)
    
3. order-service URL을 주소체계가 아닌 Microservice name으로 호출
    - user-service는 user-service.yml에서 order-service의 url을 불러온다.
    order_service.url = `‘127.0.0.1:8000/~~’` 
    → 서비스 이름, Port가 변경되어도 수정 할 필요 없도록 Eureka에 등록 된 서비스 이름으로 변경
    
    1) @LoadBalance 어노테이션 추가
    
    ```jsx
    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
    ```
    
    2) user-service.yml 수정
    
    ```jsx
    order_service:
      url: http://ORDER-SERVICE/order-service/%s/orders
    ```
    

### 2. Feign Client

> RESTful 웹 서비스와의 통신을 단순화하고 편리하게 만들어 클라이언트 코드에서 서버 간 통신을 쉽게 구현할 수 있다.
> 

1) openfeign Dependency 추가

```jsx
// openfeign
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
```

2) OrderService Interface 생성

```jsx
@FeignClient(name = "order-service") // order-service라는 이름의 서비스를 찾아서 호출
public interface OrderServiceClient {

    @GetMapping("/order-service/{userId}/orders")
    List<ResponseOrder> getOrders(@PathVariable String userId);
}
```

3) Service 수정

```jsx
@Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);

        if(userEntity == null)
            throw new UsernameNotFoundException("User not found");

        UserDto userDto = new UserDto(userEntity);

        /* Using as rest template */
//        String orderUrl = String.format(env.getProperty("order_service.url"), userId);
//        ResponseEntity<List<ResponseOrder>> orderListResponse =
//                restTemplate.exchange(orderUrl, HttpMethod.GET, null,
//                                        new ParameterizedTypeReference<List<ResponseOrder>>() {
//                });
//        List<ResponseOrder> orders = orderListResponse.getBody();

        /* Using as Feign Client */
        List<ResponseOrder> orders = orderServiceClient.getOrders(userId);

        userDto.setOrders(orders);

        return userDto;
    }
```

### Feign Client - Log출력 / 예외처리

- Log 출력
    
    1) yml 로깅 레벨 설정
    
    ```jsx
    logging:
      level:
        com.example.userservice.client: DEBUG # com.example.userservice.client 패키지의 로그를 DEBUG 레벨로 출력
    ```
    
    2) Bean 등록
    
    ```jsx
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    ```
    
    - 로그 확인
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/2b56d1d2-8f4b-405c-8f53-8d3b369fff93/Untitled.png)
    

- 에러 처리
    
    Feign Client로 잘못된 주소 요청 시
    
    - 500 Error 발생
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/1914ac9f-81f2-4b64-9737-4c4360fd9c04/Untitled.png)
    
    - 예외처리
    
    ```jsx
    List<ResponseOrder> orders = null;
    
    try {
        orders = orderServiceClient.getOrders(userId);
    } catch (FeignException ex) {
        log.error(ex.getMessage());
    }
    ```
    
    ---
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/91909f77-5837-442c-b33f-36f0875d219c/Untitled.png)
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/5c8dd114-9014-4636-8e69-5dbfa4b41c2f/Untitled.png)
    
    → 잘못된 주소로 호출하지 않은 orders 정보 외에 user에 관한 정보는 정상적으로 출력
    
    - FeignErrorDecoder를 통한 에러 처리
        
        : ErrorDecoder.decode()를 사용하여 FeignClient에서 발생한 에러의 상태코드에 따라 적절한 처리 가능
        
        ```jsx
        //FeignErrorDecoder 클래스 생성
        public class FeignErrorDecoder implements ErrorDecoder {
            @Override
            public Exception decode(String methodKey, Response response) {
                switch (response.status()) {
                    case 400:
                        break;
                    case 404:
                        if (methodKey.contains("getOrders")) {
                            return new ResponseStatusException(HttpStatus.valueOf(response.status()),
                                    "User's orders is not found");
                        }
                        break;
                    default:
                        return new Exception(response.reason());
                }
                return null;
            }
        }
        ```
        
        ```jsx
        //Bean 등록
        @Bean
        public FeignErrorDecoder getFeignErrorDecoder() {
            return new FeignErrorDecoder();
        }
        ```
        
        ```jsx
        //UserServiceImpl 메서드 수정
        @Override
            public UserDto getUserByUserId(String userId) {
                UserEntity userEntity = userRepository.findByUserId(userId);
        
                if(userEntity == null)
                    throw new UsernameNotFoundException("User not found");
        
                UserDto userDto = new UserDto(userEntity);
        
                /* Using as rest template */
        //        String orderUrl = String.format(env.getProperty("order_service.url"), userId);
        //        ResponseEntity<List<ResponseOrder>> orderListResponse =
        //                restTemplate.exchange(orderUrl, HttpMethod.GET, null,
        //                                        new ParameterizedTypeReference<List<ResponseOrder>>() {
        //                });
        //        List<ResponseOrder> orders = orderListResponse.getBody();
        
                /* Using as Feign Client: Feign exception handling */
        //        List<ResponseOrder> orders = null;
        //        try {
        //            orders = orderServiceClient.getOrders(userId);
        //        } catch (FeignException ex) {
        //            log.error(ex.getMessage());
        //        }
        
                /* ErrorDecoder */
                List<ResponseOrder> orders = orderServiceClient.getOrders(userId);
                userDto.setOrders(orders);
                
                return userDto;
            }
        ```
        
        → postman 호출
        
        ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/3b2d469c-1be4-4aa0-9c92-b6e679309ca3/Untitled.png)
        

### 예외처리 메시지 설정정보 등록

1) user-service.yml 에 에러 메시지 추가

```jsx
exception:
    order_is_empty: User's orders is empty.
```

2) 에러 메시지 : 하드코딩 → env.getProperty로

3) ErrorDecoder → env 주입, @Component로 등록

4) application 파일

5) 메인클래스에서 등록한 ErrorDecode bean 삭제 → env

# 데이터 동기화 문제

- h2-database를 in-memory로 실행 `jdbc:h2:mem:testdb`
- Order-Service 인스턴스를 2개 기동
    
    ![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/d8c923b3-0c3e-41ab-b884-cbf76bc48a27/c070c07f-bd44-4b27-81c8-2fd91d349255/Untitled.png)
    
- postman으로 4번 order 요청을 해보면 apigateway에 의해 RR방식으로 호출되어 각각의 인스턴스에 2개의 데이터가 저장되어 있다.
- 한명의 유저로 4번 주문내역을 생성해도 조회 시 호출된 인스턴스의 DB 데이터만 보여준다.

→ 해결방법

1. 각 인스턴스가 하나의 DB를 사용
2. 각각의 DB가 메시지 큐잉서버를 통해 동기화
3. 하나의 서버와 서비스 사이에 메시지 큐잉서버를 두어 서비스는 메시지 큐잉서버로 데이터를 전달하고 메시지 큐잉서버는 DB에 일괄적으로 넣는 역할

# 데이터 동기화를 위한 Kafka 사용

### Kafaka

- Producer / Consumer 분리
- 메시지를 여러 Consumer에게 전달
- 클러스터링 구조(여러 브로커로 구성된 분산 시스템)로 안정성 확보 및 확장 용이

<aside>
💡 **Apache Kafka**                                       메시지 = text, json, xml, object 등 여러 포맷의 데이터
**-** Scalar 언어로 된 오픈 소스 메시지 브로커(특정한 리소스에서 다른쪽의 서비스, 시스템으로 
  메시지를 전달할 때 사용되는 서버) 프로젝트
- 데이터를 보내는 쪽, 받는 쪽으로 구분하여 원하는 쪽으로 안전하게 메시지를 전달해주는 역할
- RabbitMQ와 비교하여 높은 데이터 처리량, 낮은 지연 시간

ex) 
| ——- Producer ———-|                          | ————-- Consumer ———-——-|
Oracle / MySql / App 등  →  Kafka  →  Hadoop / Search Engine / Monitoring 등        
                                               
End-to-End 연결방식의 아키텍처에서 프로젝트가 커질수록 서로 다른 데이터 Pipeline 연결 구조로 인해 데이터 연동의 복잡성이 증가하고 확장이 어려움
→ 모든 시스템으로 데이터를 실시간으로 전송하여 처리할 수 있는 시스템의 필요성이 생김

</aside>

Kafka Broker - 실행 된 Kafka 애플리케이션 서버

- 일반적으로 3대 이상의 Broker Cluster로 구성하는 것을 권장(안정성)
n개의 Broker 중 1대는 Controller 기능 수행(각 브로커의 담당 파티션 할당, 모니터링)
- 분산 시스템을 관리하기 위한 코디네이터를 연동해서 사용(Apache Zookeper)

메시지 저장 → Kafka Broker 

Broker 관리(Broker ID, Controller ID 등) → Zookeeper

### 

메시지는 텍스트를 비롯한 json, xml, object 등의 여러 포맷의 데이터

# 장애 처리와 Microservice 분산 추적

# Microservice Architecture 패턴

# 애플리케이션 배포를 위한 컨테이너 가상화

# 애플리케이션 배포 - Docker Container
