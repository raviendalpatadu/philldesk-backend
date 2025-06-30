# PostgreSQL Database Setup for PhillDesk

## Prerequisites
1. **Install PostgreSQL** on your system
   - Download from: https://www.postgresql.org/download/
   - Or use Docker: `docker run --name philldesk-postgres -e POSTGRES_PASSWORD=your_password -p 5432:5432 -d postgres`

## Database Setup Steps

### 1. Connect to PostgreSQL
```bash
# Using psql command line
psql -U postgres -h localhost

# Or using pgAdmin GUI tool
```

### 2. Create Database and User
```sql
-- Create database
CREATE DATABASE philldesk_db;

-- Create user (optional - you can use default postgres user)
CREATE USER philldesk_user WITH PASSWORD 'secure_password_123';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE philldesk_db TO philldesk_user;
GRANT ALL ON SCHEMA public TO philldesk_user;
```

### 3. Update Configuration
Update `application-prod.properties` with your actual database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/philldesk_db
spring.datasource.username=philldesk_user
spring.datasource.password=secure_password_123
```

## Running with PostgreSQL

### Option 1: Run with Production Profile
```bash
# Stop the current H2 application first (Ctrl+C)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Option 2: Set Environment Variables
```bash
# Windows (Command Prompt)
set SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run

# Windows (PowerShell)
$env:SPRING_PROFILES_ACTIVE="prod"
mvn spring-boot:run

# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

### Option 3: Temporarily Override H2 Config
Modify `application.properties` to use PostgreSQL directly (not recommended for development).

## Database Connection Verification

### Check if PostgreSQL is Running
```bash
# Windows
netstat -an | findstr :5432

# Linux/Mac
netstat -an | grep :5432

# Or check PostgreSQL service status
```

### Test Connection
```bash
# Using psql
psql -U postgres -h localhost -p 5432 -d philldesk_db

# Using telnet
telnet localhost 5432
```

## Docker Setup (Alternative)

If you prefer using Docker for PostgreSQL:

```bash
# Create and run PostgreSQL container
docker run --name philldesk-postgres \
  -e POSTGRES_DB=philldesk_db \
  -e POSTGRES_USER=philldesk_user \
  -e POSTGRES_PASSWORD=secure_password_123 \
  -p 5432:5432 \
  -d postgres:15

# Verify container is running
docker ps

# Connect to database
docker exec -it philldesk-postgres psql -U philldesk_user -d philldesk_db
```

## Configuration Profiles Summary

- **Default Profile** (`application.properties`): H2 in-memory database for development
- **Production Profile** (`application-prod.properties`): PostgreSQL for production
- **Test Profile** (`application-test.properties`): H2 in-memory database for testing

## Security Notes

1. **Never commit real passwords** to version control
2. **Use environment variables** for sensitive data in production:
   ```properties
   spring.datasource.password=${DB_PASSWORD:default_password}
   ```
3. **Use proper SSL** connections in production
4. **Regular database backups** are recommended

## Troubleshooting

### Common Issues:
1. **Connection refused**: PostgreSQL service not running
2. **Authentication failed**: Wrong username/password
3. **Database does not exist**: Create database first
4. **Permission denied**: Grant proper privileges to user

### Logs to Check:
- Application logs for Spring Boot errors
- PostgreSQL logs (usually in `/var/log/postgresql/` or Windows Event Logs)
