FROM openjdk:8-jdk-alpine
RUN mkdir /storage
ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ENTRYPOINT java ${JAVA_OPTS} -jar /app.jar --port=80 --spring.rabbitmq.host=rabbitmq --eureka.uri=http://eureka:8761/eureka --storage.path=/storage