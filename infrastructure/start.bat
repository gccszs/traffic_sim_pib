@echo off
REM Traffic Simulation System Infrastructure Startup Script (Windows)

echo ==========================================
echo Traffic Simulation System Infrastructure Startup Script
echo ==========================================

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose is not installed.
    pause
    exit /b 1
)

echo [SUCCESS] Docker environment check passed
echo.

REM Start services
echo Starting services...
docker-compose up -d

REM Wait for services to start
echo.
echo Waiting for services to start...
timeout /t 10 /nobreak >nul

REM Check service status
echo.
echo ==========================================
echo Service Status:
echo ==========================================
docker-compose ps

echo.
echo ==========================================
echo Service Access URLs:
echo ==========================================
echo MySQL:        localhost:3306
echo MongoDB:      localhost:27017
echo Redis:        localhost:6379
echo Kafka:        localhost:9092
echo.
echo Management Tools:
echo phpMyAdmin:   http://localhost:8083
echo Mongo Express: http://localhost:8084
echo Redis Commander: http://localhost:8082
echo Kafka UI:     http://localhost:8081
echo.
echo ==========================================
echo [SUCCESS] All services have been started
echo ==========================================
pause
