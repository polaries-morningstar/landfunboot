#!/bin/bash
# Usage: run from project root or from scripts/
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

BACKEND_PORT=8080
FRONTEND_PORT=5173
PID_FILE="$ROOT_DIR/.app.pids"

echo "Stopping application..."

# 1. Stop via PID file
if [ -f "$PID_FILE" ]; then
    while read -r pid; do
        if kill -0 "$pid" 2>/dev/null; then
            echo "Killing PID $pid..."
            kill "$pid"
            sleep 1
            kill -0 "$pid" 2>/dev/null && kill -9 "$pid"
        fi
    done < "$PID_FILE"
    rm "$PID_FILE"
fi

# 2. Fallback: kill by port
stop_by_port() {
    local pid
    pid=$(lsof -t -i:"$1" -sTCP:LISTEN 2>/dev/null)
    if [ -n "$pid" ]; then
        echo "Stopping leftover $2 on port $1 (PID: $pid)..."
        kill "$pid" && sleep 1
        kill -9 "$pid" 2>/dev/null
    fi
}

stop_by_port $BACKEND_PORT "Backend"
stop_by_port $FRONTEND_PORT "Frontend"

echo "Application stopped."
