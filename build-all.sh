#!/bin/bash

# Build script for Hospital Management Microservices

set -e  # Exit on error

echo "========================================="
echo "Building Hospital Management Microservices"
echo "========================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Step 1: Cleaning previous builds...${NC}"
./mvnw clean

echo -e "${BLUE}Step 2: Building hospital-common (shared library)...${NC}"
./mvnw install -pl hospital-common -DskipTests

echo -e "${BLUE}Step 3: Building Config Server...${NC}"
./mvnw install -pl config-server -DskipTests

echo -e "${BLUE}Step 4: Building Eureka Server...${NC}"
./mvnw install -pl eureka-server -DskipTests

echo -e "${BLUE}Step 5: Building API Gateway...${NC}"
./mvnw install -pl api-gateway -DskipTests

echo -e "${GREEN}========================================="
echo -e "Build completed successfully!"
echo -e "=========================================${NC}"
echo ""
echo "Next steps:"
echo "1. Ensure Docker services are running: docker-compose up -d"
echo "2. Start Config Server: cd config-server && ../mvnw spring-boot:run"
echo "3. Start Eureka Server: cd eureka-server && ../mvnw spring-boot:run"
echo "4. Start API Gateway: cd api-gateway && ../mvnw spring-boot:run"
echo ""
echo "Access points:"
echo "- Eureka: http://localhost:8761"
echo "- API Gateway: http://localhost:8080"
echo "- RabbitMQ: http://localhost:15672"
