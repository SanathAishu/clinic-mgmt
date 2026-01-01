FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Hospital Management System"
LABEL service="config-server"

WORKDIR /app

# Add wget for healthcheck
RUN apk add --no-cache wget

# Copy the jar file
COPY target/*.jar app.jar

# Expose port
EXPOSE 8888

# Set JVM options for container
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
