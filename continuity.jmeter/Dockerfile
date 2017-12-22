FROM openjdk:8-jdk
VOLUME /tmp

ARG JAR_FILE
ADD ${JAR_FILE} /bin/app.jar

COPY lib/ lib/
COPY bin/ bin/

ENTRYPOINT ["java","-jar", "/bin/app.jar", "--port=80", "--spring.rabbitmq.host=rabbitmq", "--eureka.uri=http://eureka:8761/eureka"]