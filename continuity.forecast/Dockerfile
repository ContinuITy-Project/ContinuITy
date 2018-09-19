FROM rocker/r-base:latest
VOLUME /tmp
VOLUME /storage

## Install Java 
RUN apt-get update \
    && apt-get install -y openjdk-8-jdk r-cran-rjava libcurl4-openssl-dev\
    && R CMD javareconf

ENV LD_LIBRARY_PATH /usr/lib/R/site-library/rJava/jri/
ENV R_HOME=/usr/lib/R

## Install R packages
RUN mkdir /r-library
RUN R -e "install.packages(c('xgboost', 'cluster', 'forecast', 'e1071', 'prophet'), repos='https://ftp.fau.de/cran/')"

COPY telescope-multi/ telescope-multi/
COPY prophet/ prophet/
ARG JAR_FILE
ADD ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar", "--port=80", "--spring.rabbitmq.host=rabbitmq", "--eureka.uri=http://eureka:8761/eureka", "--persist.path=/persisted"]