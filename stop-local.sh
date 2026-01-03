#!/bin/bash

##############################################################################
# Shutdown Script for Hospital Management System
#
# Stops all running services in the correct order:
# 1. Quarkus microservices (graceful shutdown)
# 2. Docker containers (PostgreSQL, Redis, RabbitMQ, Consul, Prometheus, Grafana)
##############################################################################

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Stopping Hospital Management System                         ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"

# ============================================================================
# Step 1: Stop Quarkus Microservices
# ============================================================================
echo -e "\n${YELLOW}[1/2] Stopping Quarkus microservices...${NC}"

# Load PIDs from file if they exist
if [ -f /tmp/hospital-services.pids ]; then
    source /tmp/hospital-services.pids
fi

# Kill microservice processes by name and port
echo "Stopping auth-service (port 8081)..."
pkill -f "auth-service.*quarkusDev" 2>/dev/null || true
lsof -ti :8081 | xargs kill -9 2>/dev/null || true
echo -e "${GREEN}✓ auth-service stopped${NC}"

echo "Stopping audit-service (port 8088)..."
pkill -f "audit-service.*quarkusDev" 2>/dev/null || true
lsof -ti :8088 | xargs kill -9 2>/dev/null || true
echo -e "${GREEN}✓ audit-service stopped${NC}"

echo "Stopping api-gateway (port 8080)..."
pkill -f "api-gateway.*quarkusDev" 2>/dev/null || true
lsof -ti :8080 | xargs kill -9 2>/dev/null || true
echo -e "${GREEN}✓ api-gateway stopped${NC}"

sleep 2

# ============================================================================
# Step 2: Stop Docker Containers
# ============================================================================
echo -e "\n${YELLOW}[2/2] Stopping Docker containers...${NC}"

cd "$PROJECT_DIR/docker"

docker-compose down 2>/dev/null || true
echo -e "${GREEN}✓ All Docker containers stopped${NC}"

cd "$PROJECT_DIR"

# ============================================================================
# Cleanup
# ============================================================================
rm -f /tmp/hospital-services.pids
rm -f /tmp/auth-service.log
rm -f /tmp/audit-service.log
rm -f /tmp/api-gateway.log

# ============================================================================
# Summary
# ============================================================================
echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  SHUTDOWN COMPLETE                            ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"

echo -e "\n${GREEN}All services have been stopped.${NC}"
echo -e "\n${YELLOW}To start services again:${NC} ./start-local.sh"
