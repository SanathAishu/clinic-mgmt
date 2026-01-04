#!/bin/bash

# Stop all locally running services

echo "=========================================="
echo "Hospital Management System - Stop Services"
echo "=========================================="

# Service ports for orphan cleanup
PORTS=(8761 8080 8081 8082 8083 8084 8085 8086 8087 8088)

# Stop service by PID file
stop_service() {
    local pidfile=$1
    local name=$(basename "$pidfile" .pid)

    if [ ! -f "$pidfile" ]; then
        return 1
    fi

    local pid=$(cat "$pidfile")

    if ! kill -0 "$pid" 2>/dev/null; then
        echo "  $name: Already stopped"
        rm -f "$pidfile"
        return 0
    fi

    echo -n "  Stopping $name (PID: $pid)..."

    # Send SIGTERM for graceful shutdown
    kill "$pid" 2>/dev/null

    # Wait up to 10 seconds for graceful shutdown
    local count=0
    while kill -0 "$pid" 2>/dev/null && [ $count -lt 10 ]; do
        sleep 1
        count=$((count + 1))
        echo -n "."
    done

    # Force kill if still running
    if kill -0 "$pid" 2>/dev/null; then
        echo -n " forcing..."
        kill -9 "$pid" 2>/dev/null
        sleep 1
    fi

    if kill -0 "$pid" 2>/dev/null; then
        echo " FAILED"
        return 1
    else
        echo " stopped"
        rm -f "$pidfile"
        return 0
    fi
}

# Kill any orphan processes on service ports
kill_orphan_on_port() {
    local port=$1
    local pid=$(lsof -ti:$port 2>/dev/null)

    if [ -n "$pid" ]; then
        echo "  Killing orphan process on port $port (PID: $pid)"
        kill -9 $pid 2>/dev/null
    fi
}

echo ""
echo "Stopping services from PID files..."

# Stop services in reverse order (business services first, then gateway, then eureka)
services_order=(
    "audit-service"
    "notification-service"
    "facility-service"
    "medical-records-service"
    "appointment-service"
    "doctor-service"
    "patient-service"
    "auth-service"
    "api-gateway"
    "eureka-server"
)

for service in "${services_order[@]}"; do
    pidfile="logs/${service}.pid"
    if [ -f "$pidfile" ]; then
        stop_service "$pidfile"
    fi
done

# Stop any remaining services from PID files
for pidfile in logs/*.pid; do
    if [ -f "$pidfile" ]; then
        stop_service "$pidfile"
    fi
done

echo ""
echo "Checking for orphan processes on service ports..."

for port in "${PORTS[@]}"; do
    kill_orphan_on_port $port
done

echo ""
echo "=========================================="
echo "All services stopped!"
echo "=========================================="
echo ""
echo "Infrastructure (Docker containers) still running."
echo "To stop infrastructure: docker-compose down"
echo ""
