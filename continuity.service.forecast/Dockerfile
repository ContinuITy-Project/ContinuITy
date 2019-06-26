FROM continuityproject/forecast-base-image

COPY telescope-multi/ telescope-multi/
COPY prophet/ prophet/
ARG JAR_FILE
ADD ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar", "--port=80", "--spring.rabbitmq.host=rabbitmq", "--eureka.uri=http://eureka:8761/eureka", "--persist.path=/persisted"]