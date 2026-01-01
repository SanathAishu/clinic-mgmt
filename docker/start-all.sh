#!/bin/bash

# Start all services in a single container
# Services are started sequentially with proper wait times

JAVA_OPTS="-Xms64m -Xmx128m -XX:+UseG1GC -XX:+UseContainerSupport"

echo "=========================================="
echo "Starting Hospital Management System"
echo "=========================================="

# Function to wait for a service to be healthy
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=30
    local attempt=1

    echo "Waiting for $name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "$name is ready!"
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    echo "WARNING: $name may not be ready, continuing anyway..."
    return 1
}

# 1. Start Config Server
echo "[1/11] Starting Config Server..."
java $JAVA_OPTS -jar /app/config-server.jar --server.port=8888 &
sleep 10

# 2. Start Eureka Server
echo "[2/11] Starting Eureka Server..."
java $JAVA_OPTS -jar /app/eureka-server.jar --server.port=8761 \
    --spring.config.import=optional:configserver:http://localhost:8888 &
wait_for_service "http://localhost:8761/actuator/health" "Eureka Server"

# 3. Start API Gateway
echo "[3/11] Starting API Gateway..."
java $JAVA_OPTS -jar /app/api-gateway.jar --server.port=8080 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 5

# 4. Start Auth Service
echo "[4/11] Starting Auth Service..."
java $JAVA_OPTS -jar /app/auth-service.jar --server.port=8081 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 5. Start Patient Service
echo "[5/11] Starting Patient Service..."
java $JAVA_OPTS -jar /app/patient-service.jar --server.port=8082 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 6. Start Doctor Service
echo "[6/11] Starting Doctor Service..."
java $JAVA_OPTS -jar /app/doctor-service.jar --server.port=8083 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 7. Start Appointment Service
echo "[7/11] Starting Appointment Service..."
java $JAVA_OPTS -jar /app/appointment-service.jar --server.port=8084 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 8. Start Medical Records Service
echo "[8/11] Starting Medical Records Service..."
java $JAVA_OPTS -jar /app/medical-records-service.jar --server.port=8085 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 9. Start Facility Service
echo "[9/11] Starting Facility Service..."
java $JAVA_OPTS -jar /app/facility-service.jar --server.port=8086 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 10. Start Notification Service
echo "[10/11] Starting Notification Service..."
java $JAVA_OPTS -jar /app/notification-service.jar --server.port=8087 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &
sleep 3

# 11. Start Audit Service
echo "[11/11] Starting Audit Service..."
java $JAVA_OPTS -jar /app/audit-service.jar --server.port=8088 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ &

echo "=========================================="
echo "All services started!"
echo "=========================================="
echo "API Gateway:    http://localhost:8080"
echo "Eureka:         http://localhost:8761"
echo "Auth Service:   http://localhost:8081"
echo "=========================================="

# Keep container running
wait
