#!/bin/bash

# Local development script
# Runs infrastructure in Docker, services natively

set -e

JAVA_OPTS="-Xms64m -Xmx128m -XX:+UseG1GC"

echo "=========================================="
echo "Hospital Management System - Local Dev"
echo "=========================================="

# Check if infrastructure is running
check_infra() {
    if ! docker ps | grep -q hospital-postgres; then
        echo "Starting infrastructure..."
        docker-compose up -d
        echo "Waiting for infrastructure to be ready..."
        sleep 15
    else
        echo "Infrastructure already running."
    fi
}

# Build if needed
build_if_needed() {
    if [ ! -f "eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar" ]; then
        echo "Building all services..."
        ./mvnw clean package -DskipTests -q
    fi
}

# Wait for a service to be healthy
wait_for_service() {
    local url=$1
    local name=$2
    local max=20
    local count=0

    echo "Waiting for $name..."
    while [ $count -lt $max ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "$name is ready!"
            return 0
        fi
        sleep 2
        count=$((count + 1))
    done
    echo "WARNING: $name may not be ready"
    return 0
}

# Start a service in background
start_service() {
    local name=$1
    local jar=$2
    local port=$3
    local db_name=${name//-/_}

    echo "Starting $name on port $port..."
    java $JAVA_OPTS -Dspring.amqp.deserialization.trust.all=true -jar $jar \
        --server.port=$port \
        --spring.datasource.url=jdbc:postgresql://localhost:5432/$db_name \
        --spring.datasource.username=postgres \
        --spring.datasource.password=postgres \
        --spring.jpa.hibernate.ddl-auto=update \
        --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
        --spring.rabbitmq.host=localhost \
        --spring.rabbitmq.username=guest \
        --spring.rabbitmq.password=guest \
        --spring.data.redis.host=localhost \
        --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ \
        --jwt.secret=HospitalManagementSystemSecretKeyForHS512Algorithm2024SecureTokenGeneration \
        > logs/$name.log 2>&1 &

    echo $! > logs/$name.pid
}

# Main
mkdir -p logs

check_infra
build_if_needed

echo ""
echo "Starting services (logs in ./logs/)..."
echo ""

# Start Eureka Server
echo "Starting eureka-server on port 8761..."
java $JAVA_OPTS -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar \
    --server.port=8761 \
    > logs/eureka-server.log 2>&1 &
echo $! > logs/eureka-server.pid
wait_for_service "http://localhost:8761/actuator/health" "Eureka"

# Start API Gateway
echo "Starting api-gateway on port 8080..."
java $JAVA_OPTS -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar \
    --server.port=8080 \
    --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/ \
    --jwt.secret=HospitalManagementSystemSecretKeyForHS512Algorithm2024SecureTokenGeneration \
    > logs/api-gateway.log 2>&1 &
echo $! > logs/api-gateway.pid
sleep 5

# Start business services
start_service "auth-service" "auth-service/target/auth-service-1.0.0-SNAPSHOT.jar" 8081
start_service "patient-service" "patient-service/target/patient-service-1.0.0-SNAPSHOT.jar" 8082
start_service "doctor-service" "doctor-service/target/doctor-service-1.0.0-SNAPSHOT.jar" 8083
sleep 3

start_service "appointment-service" "appointment-service/target/appointment-service-1.0.0-SNAPSHOT.jar" 8084
start_service "medical-records-service" "medical-records-service/target/medical-records-service-1.0.0-SNAPSHOT.jar" 8085
start_service "facility-service" "facility-service/target/facility-service-1.0.0-SNAPSHOT.jar" 8086
sleep 3

start_service "notification-service" "notification-service/target/notification-service-1.0.0-SNAPSHOT.jar" 8087
start_service "audit-service" "audit-service/target/audit-service-1.0.0-SNAPSHOT.jar" 8088

echo ""
echo "=========================================="
echo "All services starting!"
echo "=========================================="
echo ""
echo "Endpoints:"
echo "  API Gateway:  http://localhost:8080"
echo "  Eureka:       http://localhost:8761"
echo "  RabbitMQ:     http://localhost:15672"
echo ""
echo "Logs: ./logs/<service>.log"
echo "PIDs: ./logs/<service>.pid"
echo ""
echo "To stop: ./stop-local.sh"
echo "=========================================="
