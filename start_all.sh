#!/bin/bash

# Configuration
BACKEND_PORT=8080
FRONTEND_PORT=5173
PID_FILE=".app.pids"

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        return 0 # Port is in use
    else
        return 1 # Port is free
    fi
}

# Cleanup old logs
echo "Cleaning up old logs..."
rm -f backend.log frontend.log

# Check backend port
if check_port $BACKEND_PORT; then
    echo "Error: Backend port $BACKEND_PORT is already in use."
    echo "Please run ./stop_all.sh first."
    exit 1
fi

# Check frontend port
if check_port $FRONTEND_PORT; then
    echo "Error: Frontend port $FRONTEND_PORT is already in use."
    echo "Please run ./stop_all.sh first."
    exit 1
fi

# Start Backend
echo "Starting Backend (Spring Boot)..."
mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > $PID_FILE

# Start Frontend
echo "Starting Frontend (Vite)..."
if [ -d "frontend" ]; then
    cd frontend
    npm run dev -- --host > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID >> ../$PID_FILE
    cd ..
else
    echo "Error: frontend directory not found."
    ./stop_all.sh
    exit 1
fi

echo "------------------------------------------------"
echo "Application is starting..."
echo "Backend: http://localhost:$BACKEND_PORT"
echo "Frontend: http://localhost:$FRONTEND_PORT"
echo "------------------------------------------------"
echo "Logs are being written to backend.log and frontend.log"
echo "Use './stop_all.sh' to stop the application."
echo "------------------------------------------------"

# Optional: wait a bit and check if they are still running
sleep 2
if kill -0 $BACKEND_PID 2>/dev/null && kill -0 $FRONTEND_PID 2>/dev/null; then
    echo "Processes started successfully (PIDs: $BACKEND_PID, $FRONTEND_PID)"
else
    echo "Warning: One or more processes failed to start or exited early."
    echo "Please check backend.log and frontend.log for details."
fi
