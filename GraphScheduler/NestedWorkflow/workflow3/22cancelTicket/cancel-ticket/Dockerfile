# Use an official OpenJDK runtime as a parent image
FROM openjdk:8-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built JAR file to the container
COPY build/libs/cancel-ticket.jar /app/server.jar

# Expose the port that the app runs on
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "server.jar"]


# docker build -t cancel-ticket .
# docker run cancel-ticket