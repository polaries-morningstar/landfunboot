#!/bin/bash

PID_FILE=".app.pids"
BACKEND_PORT=8080
FRONTEND_PORT=5173

echo "Stopping application..."

# 1. Try stopping via PID file
if [ -f $PID_FILE ]; then
    echo "Found PID file, stopping processes..."
    while read -r pid; do
        if kill -0 "$pid" 2>/dev/null; then
            echo "Killing process $pid..."
            kill "$pid"
            sleep 1
            if kill -0 "$pid" 2>/dev/null; then
                echo "Force killing process $pid..."
                kill -9 "$pid"
            fi
        else
            echo "Process $pid is not running."
        fi
    done < "$PID_FILE"
    rm "$PID_FILE"
fi

# 2. Double check by port (just in case)
stop_by_port() {
    local port=$1
    local name=$2
    local pid=$(lsof -t -i:$port -sTCP:LISTEN)
    if [ -n "$pid" ]; then
        echo "Cleaning up leftover $name on port $port (PID: $pid)..."
        kill "$pid"
        sleep 1
        kill -9 "$pid" 2>/dev/null
    fi
}

stop_by_port $BACKEND_PORT "Backend"
stop_by_port $FRONTEND_PORT "Frontend"

echo "Application stopped."
