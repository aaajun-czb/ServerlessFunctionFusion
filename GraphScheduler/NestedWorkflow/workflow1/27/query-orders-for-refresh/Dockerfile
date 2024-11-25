# Use an official OpenJDK runtime as a parent image
FROM openjdk:8-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built JAR file to the container
COPY build/libs/query-orders-for-refresh.jar /app/query-orders-for-refresh.jar

# Run the JAR file
ENTRYPOINT ["java", "-jar", "query-orders-for-refresh.jar"]

# docker build -t query-orders-for-refresh .
# docker run query-orders-for-refresh