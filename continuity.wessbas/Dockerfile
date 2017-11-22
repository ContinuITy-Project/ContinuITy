FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
ADD ${JAR_FILE} app.jar

COPY configuration/ configuration/

ENTRYPOINT ["java","-jar","/app.jar", "--port=80"]