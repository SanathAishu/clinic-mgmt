#!/bin/bash

# Local development script
# Runs infrastructure in Docker, services natively with nohup

set -e

JAVA_OPTS="-Xms64m -Xmx128m -XX:+UseG1GC"
JWT_SECRET="HospitalManagementSystemSecretKeyForHS512Algorithm2024SecureTokenGeneration"
EUREKA_URL="http://localhost:8761/eureka/"

echo "=========================================="
echo "Hospital Management System - Local Dev"
echo "=========================================="

# Check if infrastructure is running
check_infra() {
    if ! docker ps | grep -q hospital-postgres; then
        echo "Starting infrastructure..."
        docker start hospital-postgres hospital-redis 2>/dev/null || docker-compose up -d postgres redis
        echo "Waiting for infrastructure to be ready..."
        sleep 10
    else
        echo "Infrastructure already running."
        docker start hospital-redis 2>/dev/null || true
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
    local max=${3:-30}
    local count=0

    echo -n "Waiting for $name"
    while [ $count -lt $max ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo " ready!"
            return 0
        fi
        echo -n "."
        sleep 2
        count=$((count + 1))
    done
    echo " timeout (may still be starting)"
    return 0
}

# Start a service in background with nohup
start_service() {
    local name=$1
    local jar=$2
    local port=$3

    echo "Starting $name on port $port..."
    nohup java $JAVA_OPTS -jar "$jar" \
        --server.port=$port \
        --spring.data.redis.host=localhost \
        --eureka.client.service-url.defaultZone=$EUREKA_URL \
        --jwt.secret=$JWT_SECRET \
        > "logs/$name.log" 2>&1 &

    echo $! > "logs/$name.pid"
    echo "  PID: $(cat logs/$name.pid)"
}

# Main
mkdir -p logs

# Stop any existing services first
if [ -f "logs/eureka-server.pid" ]; then
    echo "Stopping existing services..."
    ./stop-local.sh 2>/dev/null || true
    sleep 2
fi

check_infra
build_if_needed

echo ""
echo "Starting services (logs in ./logs/)..."
echo ""

# Start Eureka Server
echo "Starting eureka-server on port 8761..."
nohup java $JAVA_OPTS -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar \
    --server.port=8761 \
    > logs/eureka-server.log 2>&1 &
echo $! > logs/eureka-server.pid
echo "  PID: $(cat logs/eureka-server.pid)"
wait_for_service "http://localhost:8761/actuator/health" "Eureka" 30

# Start API Gateway
echo "Starting api-gateway on port 8080..."
nohup java $JAVA_OPTS -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar \
    --server.port=8080 \
    --eureka.client.service-url.defaultZone=$EUREKA_URL \
    --jwt.secret=$JWT_SECRET \
    > logs/api-gateway.log 2>&1 &
echo $! > logs/api-gateway.pid
echo "  PID: $(cat logs/api-gateway.pid)"
sleep 3

# Start business services in batches
echo ""
echo "Starting business services..."

start_service "auth-service" "auth-service/target/auth-service-1.0.0-SNAPSHOT.jar" 8081
start_service "patient-service" "patient-service/target/patient-service-1.0.0-SNAPSHOT.jar" 8082
start_service "doctor-service" "doctor-service/target/doctor-service-1.0.0-SNAPSHOT.jar" 8083
sleep 2

start_service "appointment-service" "appointment-service/target/appointment-service-1.0.0-SNAPSHOT.jar" 8084
start_service "medical-records-service" "medical-records-service/target/medical-records-service-1.0.0-SNAPSHOT.jar" 8085
start_service "facility-service" "facility-service/target/facility-service-1.0.0-SNAPSHOT.jar" 8086
sleep 2

start_service "notification-service" "notification-service/target/notification-service-1.0.0-SNAPSHOT.jar" 8087
start_service "audit-service" "audit-service/target/audit-service-1.0.0-SNAPSHOT.jar" 8088

echo ""
echo "Waiting for services to initialize..."
sleep 15

# Check service health
echo ""
echo "Service Health Check:"
echo "---------------------"
for port in 8761 8080 8081 8082 8083 8084 8085 8086 8087 8088; do
    name=$(case $port in
        8761) echo "eureka-server" ;;
        8080) echo "api-gateway" ;;
        8081) echo "auth-service" ;;
        8082) echo "patient-service" ;;
        8083) echo "doctor-service" ;;
        8084) echo "appointment-service" ;;
        8085) echo "medical-records-service" ;;
        8086) echo "facility-service" ;;
        8087) echo "notification-service" ;;
        8088) echo "audit-service" ;;
    esac)
    status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null || echo "DOWN")
    if [ "$status" = "200" ]; then
        echo "  ✓ $name ($port): UP"
    else
        echo "  ✗ $name ($port): $status"
    fi
done

echo ""
echo "=========================================="
echo "All services started!"
echo "=========================================="
echo ""
echo "Endpoints:"
echo "  API Gateway:  http://localhost:8080"
echo "  Eureka:       http://localhost:8761"
echo ""
echo "Logs: ./logs/<service>.log"
echo "PIDs: ./logs/<service>.pid"
echo ""
echo "To stop: ./stop-local.sh"
echo "=========================================="
