#!/bin/bash
# Usage: run from project root or from scripts/
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

BACKEND_PORT=8080
FRONTEND_PORT=5173
PID_FILE="$ROOT_DIR/.app.pids"
LOGS_DIR="$ROOT_DIR/logs"

mkdir -p "$LOGS_DIR"

check_port() {
    lsof -Pi :"$1" -sTCP:LISTEN -t >/dev/null 2>&1
}

# Check ports
if check_port $BACKEND_PORT; then
    echo "Error: Backend port $BACKEND_PORT is already in use. Run ./scripts/stop_all.sh first."
    exit 1
fi
if check_port $FRONTEND_PORT; then
    echo "Error: Frontend port $FRONTEND_PORT is already in use. Run ./scripts/stop_all.sh first."
    exit 1
fi

# Start Backend
echo "Starting Backend (Spring Boot)..."
cd "$ROOT_DIR"
nohup java -jar target/landfunboot-*.jar > "$LOGS_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$PID_FILE"

# Start Frontend
echo "Starting Frontend (Vite)..."
if [ -d "$ROOT_DIR/frontend" ]; then
    cd "$ROOT_DIR/frontend"
    npm run dev -- --host > "$LOGS_DIR/frontend.log" 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID >> "$PID_FILE"
    cd "$ROOT_DIR"
else
    echo "Error: frontend directory not found."
    bash "$SCRIPT_DIR/stop_all.sh"
    exit 1
fi

echo "------------------------------------------------"
echo "Application is starting..."
echo "  Backend:  http://localhost:$BACKEND_PORT"
echo "  Frontend: http://localhost:$FRONTEND_PORT"
echo "  Logs:     $LOGS_DIR/"
echo "  Stop:     ./scripts/stop_all.sh"
echo "------------------------------------------------"

sleep 2
if kill -0 $BACKEND_PID 2>/dev/null && kill -0 $FRONTEND_PID 2>/dev/null; then
    echo "Started successfully (PIDs: $BACKEND_PID, $FRONTEND_PID)"
else
    echo "Warning: a process may have failed. Check logs/ for details."
fi
