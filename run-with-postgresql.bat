@echo off
echo ===================================
echo PhillDesk PostgreSQL Setup Script
echo ===================================
echo.

echo Checking if PostgreSQL is running...
netstat -an | findstr :5432 >nul
if %errorlevel% neq 0 (
    echo [ERROR] PostgreSQL is not running on port 5432
    echo.
    echo Please start PostgreSQL service or install PostgreSQL:
    echo 1. Install PostgreSQL: https://www.postgresql.org/download/windows/
    echo 2. Or use Docker: docker run --name philldesk-postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres
    echo.
    pause
    exit /b 1
)

echo [OK] PostgreSQL is running on port 5432
echo.

echo Setting up database...
echo Please run these SQL commands in your PostgreSQL client:
echo.
echo ----------------------------------------
echo CREATE DATABASE philldesk_db;
echo CREATE USER philldesk_user WITH PASSWORD 'philldesk_pass';
echo GRANT ALL PRIVILEGES ON DATABASE philldesk_db TO philldesk_user;
echo GRANT ALL ON SCHEMA public TO philldesk_user;
echo ----------------------------------------
echo.

echo Setting environment variables for this session...
set DATABASE_URL=jdbc:postgresql://localhost:5432/philldesk_db
set DATABASE_USERNAME=philldesk_user
set DATABASE_PASSWORD=philldesk_pass

echo Environment variables set:
echo DATABASE_URL=%DATABASE_URL%
echo DATABASE_USERNAME=%DATABASE_USERNAME%
echo DATABASE_PASSWORD=***hidden***
echo.

echo Starting PhillDesk backend with PostgreSQL...
echo Press Ctrl+C to stop the application
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=prod
