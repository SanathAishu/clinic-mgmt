FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Hospital Management System"
LABEL service="facility-service"

WORKDIR /app

RUN apk add --no-cache wget

COPY target/*.jar app.jar

EXPOSE 8086

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
