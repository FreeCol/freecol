FROM ubuntu:16.04

# Define your required Apache Ant version here
ENV ANT_VERSION=1.10.3
ENV ANT_HOME=/opt/ant

# Run package update
RUN apt-get update && apt-get upgrade -y

# Install core apps and Java JDK
RUN apt-get install -y software-properties-common wget git openjdk-8-jdk

# make /bin/sh symlink to bash instead of dash:
RUN echo "dash dash/sh boolean false" | debconf-set-selections
RUN DEBIAN_FRONTEND=noninteractive dpkg-reconfigure dash

# Setting up OpenJDK environment
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-amd64

# Install and setup Apache Ant
RUN wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
    && wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz.sha512 \
    && echo "$(cat apache-ant-${ANT_VERSION}-bin.tar.gz.sha512) apache-ant-${ANT_VERSION}-bin.tar.gz" | sha512sum -c \
    && tar -zvxf apache-ant-${ANT_VERSION}-bin.tar.gz -C /opt/ \
    && ln -s /opt/apache-ant-${ANT_VERSION} /opt/ant \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz.sha512

RUN update-alternatives --install "/usr/bin/ant" "ant" "/opt/ant/bin/ant" 1 && \
    update-alternatives --set "ant" "/opt/ant/bin/ant" 

ENV ANT_HOME /opt/ant

# Cleaning up
RUN apt-get autoremove && apt-get autoclean

WORKDIR /freecol

COPY . /freecol

RUN ant

# Specify the command to run your Java application
CMD ["java", "-Xmx2000M", "-jar", "FreeCol.jar"]