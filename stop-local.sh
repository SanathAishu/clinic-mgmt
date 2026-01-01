#!/bin/bash

# Stop all locally running services

echo "Stopping all services..."

for pidfile in logs/*.pid; do
    if [ -f "$pidfile" ]; then
        pid=$(cat "$pidfile")
        name=$(basename "$pidfile" .pid)
        if kill -0 "$pid" 2>/dev/null; then
            echo "Stopping $name (PID: $pid)..."
            kill "$pid" 2>/dev/null
        fi
        rm -f "$pidfile"
    fi
done

echo ""
echo "All services stopped."
echo ""
echo "To stop infrastructure: docker-compose -f docker-compose-infra.yml down"
