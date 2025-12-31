#!/bin/bash

# Startup script for Hospital Management Microservices Infrastructure

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}========================================="
echo "Hospital Management Microservices"
echo "Infrastructure Startup"
echo -e "=========================================${NC}"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}Error: Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

echo -e "${BLUE}Step 1: Starting Docker infrastructure services...${NC}"
docker-compose up -d

echo -e "${BLUE}Step 2: Waiting for services to be healthy...${NC}"
sleep 5

echo -e "${BLUE}Step 3: Verifying PostgreSQL schemas...${NC}"
docker exec hospital-postgres psql -U hospital_user -d hospital_db -c "\dn" | grep -E "(auth_service|patient_service|doctor_service|appointment_service|medical_records_service|facility_service|notification_service|audit_service)"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ All 8 PostgreSQL schemas are ready${NC}"
else
    echo -e "${YELLOW}⚠ Schemas not found. Initializing...${NC}"
    docker exec -i hospital-postgres psql -U hospital_user -d hospital_db < infrastructure/postgresql/init-schemas.sql
    echo -e "${GREEN}✓ Schemas initialized${NC}"
fi

echo -e "${BLUE}Step 4: Checking Docker services status...${NC}"
docker-compose ps

echo -e "${GREEN}========================================="
echo "Infrastructure Services Ready!"
echo -e "=========================================${NC}"
echo ""
echo "Services running:"
echo "- PostgreSQL: localhost:5432 (user: hospital_user)"
echo "- Redis: localhost:6379"
echo "- RabbitMQ: localhost:5672"
echo "- RabbitMQ Management: http://localhost:15672"
echo ""
echo "Next steps:"
echo "1. Build all services: ./build-all.sh"
echo "2. Start Config Server: cd config-server && ../mvnw spring-boot:run"
echo "3. Start Eureka Server: cd eureka-server && ../mvnw spring-boot:run"
echo "4. Start API Gateway: cd api-gateway && ../mvnw spring-boot:run"
echo ""
echo "To stop all services: docker-compose down"
