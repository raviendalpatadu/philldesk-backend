@echo off
echo Starting PhillDesk Backend with PostgreSQL...

REM Set environment variables for PostgreSQL
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=philldesk_db
set DB_USERNAME=philldesk_user
set DB_PASSWORD=philldesk_pass
set JWT_SECRET=PhillDeskSecretKeyForJWTTokenGenerationAndValidation2024
set JWT_EXPIRATION=86400000
set SPRING_PROFILES_ACTIVE=prod

echo Environment variables set:
echo DB_HOST=%DB_HOST%
echo DB_PORT=%DB_PORT%
echo DB_NAME=%DB_NAME%
echo DB_USERNAME=%DB_USERNAME%
echo SPRING_PROFILES_ACTIVE=%SPRING_PROFILES_ACTIVE%

echo.
echo Starting application with PostgreSQL...
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod

pause
