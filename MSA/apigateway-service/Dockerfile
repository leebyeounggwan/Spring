FROM openjdk:17-ea-11-slim
VOLUME /tmp
COPY build/libs/apigateway-service-1.0.jar ApigatewayService.jar
ENTRYPOINT ["java","-jar","ApigatewayService.jar"]