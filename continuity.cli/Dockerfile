FROM openjdk:8-jdk

ARG JAR_FILE
ADD ${JAR_FILE} /bin/cli.jar

ARG CONFIGURATION_FILE
ADD ${CONFIGURATION_FILE} /continuity.cli.properties

# Download and extract JMeter
ADD https://www-eu.apache.org/dist//jmeter/binaries/apache-jmeter-5.1.1.tgz /apache-jmeter-5.1.1.tgz
RUN tar -xzf /apache-jmeter-5.1.1.tgz
RUN rm /apache-jmeter-5.1.1.tgz

# Add Markov4JMeter plugin
ADD https://repository.novatec-gmbh.de/service/local/repositories/3rd_party_libs/content/net/voorn/markov4jmeter/1.0.20181115/markov4jmeter-1.0.20181115.jar /apache-jmeter-5.1.1/lib/ext/

# Add Random CSV Data Set plugin
ADD https://jmeter-plugins.org/files/packages/bzm-random-csv-0.6.zip /bzm-random-csv-0.6.zip
RUN unzip -n /bzm-random-csv-0.6.zip -d /apache-jmeter-5.1.1
RUN rm /bzm-random-csv-0.6.zip
    
# Install gedit for editing YAML files
RUN apt-get update && \
    apt-get --assume-yes install gedit

# Using gedit as default YAML editor
RUN apt-get --assume-yes --fix-missing install xdg-utils && \
    mkdir -p /root/.local/share/applications && \
    echo 'gedit $1 > /dev/null 2>&1' > /bin/gedit_silent.sh && \
    echo "[Desktop Entry]\nVersion=1.0\nEncoding=UTF-8\nType=Application\nName=gedit\nNoDisplay=true\nExec=sh /bin/gedit_silent.sh" > /root/.local/share/applications/gedit_silent.desktop && \
    xdg-mime default gedit_silent.desktop application/x-yaml

VOLUME /working-dir

ENTRYPOINT [ "java", "-jar", "/bin/cli.jar" ]