FROM openjdk:8-jdk
VOLUME /tmp
VOLUME /persisted
ARG JAR_FILE
ADD ${JAR_FILE} app.jar

COPY configuration/ configuration/

ENTRYPOINT ["java","-jar","/app.jar", "--port=80", "--spring.rabbitmq.host=rabbitmq", "--eureka.uri=http://eureka:8761/eureka", "--persist.path=/persisted"]