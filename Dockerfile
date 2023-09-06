# Stage 1: Build with JDK 8 for Ant
FROM openjdk:8 AS builder

# Install Ant
RUN apt-get update && apt-get install -y ant

WORKDIR /freecol
COPY . /freecol

FROM openjdk:11-jdk

# Use the base image with OpenJDK 11

# Update package repositories and install Ant
RUN apt-get update && \
    apt-get install -y ant && \
    apt-get install -y libxext6 libxrender1 libxtst6 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* \

# Build the application using Ant
RUN ant

ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu

# Start Xvfb with a delay
# Start Xvfb and redirect output to a log file
RUN Xvfb :1 -screen 0 1024x768x16 &
RUN sleep 5

# Set up a virtual display
ENV DISPLAY=:1
RUN Xvfb :1 -screen 0 1024x768x16 -fbdir /tmp >> /var/log/xvfb.log 2>&1 &

# Create a non-root user
RUN useradd -ms /bin/bash tneks

# Set the working directory
WORKDIR /freecol

# Copy the source code and build files into the container
COPY . /freecol

# Add Apache Commons CLI library (adjust the path accordingly)
COPY commons-cli-1.4.jar /commons-cli-1.4.jar

# Create directories for application data and configuration
RUN mkdir -p /home/tneks/.local/share && \
    chown -R tneks:tneks /home/tneks/.local && \
    mkdir -p /freecol && \
    chown -R tneks:tneks /freecol

# Switch to the non-root user
USER tneks

EXPOSE 80
# Specify the command to run the application when the container starts
CMD ["java", "-Xmx2000M", "-Djava.awt.headless=true", "-cp", "/commons-cli-1.4.jar:build/classes:build/main:build", "net.sf.freecol.FreeCol", "--headless"]
