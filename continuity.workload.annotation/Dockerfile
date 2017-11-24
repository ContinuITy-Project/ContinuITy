FROM openjdk:8-jdk-alpine
VOLUME /tmp
VOLUME /storage
ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar", "--port=80"]