# PhillDesk Backend

Spring Boot 3 (Java 17) backend for the PhillDesk Online Pharmacy Management System. Provides REST APIs for authentication (JWT), roles & users, medicines inventory, prescriptions workflow, billing, notifications, analytics and scheduled tasks.

## Tech Stack
- Java 17, Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Security
- H2 (dev/test), PostgreSQL (prod)
- JWT (io.jsonwebtoken 0.12.3)
- iText 7 (PDF generation)
- Lombok
- Maven

## Quick Start (Development – H2 + Hot Reload)
```powershell
# From project root
./mvnw spring-boot:run
# or (Windows)
mvnw.cmd spring-boot:run
```
Service starts at: http://localhost:8080

H2 file-based dev DB path: `./data/devdb.mv.db` (persisted between runs)
H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/devdb`
- User: `sa`
- Password: (empty)

## Running Tests
```powershell
mvnw.cmd test
```
The test profile uses `application-test.properties` (H2 in‑memory).

## Project Structure (Key Folders)
```
src/main/java/com/philldesk/philldeskbackend
  ├── controller/        # REST controllers (/api/...)
  ├── dto/               # Request/response DTOs
  ├── entity/            # JPA entities & enums
  ├── repository/        # Spring Data repositories
  ├── security/          # JWT, filters, user details service
  ├── service/           # Business logic services
  ├── exception/         # (Custom exceptions / handlers)
  └── config/            # (Security & other configuration classes)
src/main/resources
  ├── application.properties        # Dev (H2) defaults
  ├── application-prod.properties   # Prod (PostgreSQL) profile
  ├── schema.sql                    # Optional manual schema (PostgreSQL style)
  ├── data.sql                      # Seed data (roles, medicines, sample bills)
  ├── static/, templates/           # (If using static resources or Thymeleaf)
uploads/prescriptions               # Uploaded prescription files
```

## Profiles & Environments
| Profile | Activation | Database | Notes |
|---------|------------|----------|-------|
| (default) | none | H2 file `./data/devdb` | Dev convenience; auto schema update (ddl-auto=update) |
| prod | `-Dspring-boot.run.profiles=prod` or `SPRING_PROFILES_ACTIVE=prod` | PostgreSQL | Use env vars for credentials |
| test | auto during `mvn test` | H2 (in-memory) | Isolated test data |

### Dev (H2) Behavior
- `spring.jpa.hibernate.ddl-auto=update` keeps schema in sync with entities.
- `data.sql` runs (insert-if-not-exists logic via ON CONFLICT for Postgres syntax; harmless on H2 when compatible). 
- H2 console enabled at `/h2-console`.

### Production (PostgreSQL) Behavior
`application-prod.properties` expects environment variables (with shown defaults):
```
DATABASE_URL=jdbc:postgresql://localhost:5432/philldesk_db
DATABASE_USERNAME=philldesk_user
DATABASE_PASSWORD=philldesk_pass
PORT=8080
CONTEXT_PATH= (optional)
```
Spring resolves them via `${VARIABLE:default}` syntax.

Run with profile:
```powershell
# PowerShell
$env:SPRING_PROFILES_ACTIVE="prod"; mvnw.cmd spring-boot:run
# CMD
set SPRING_PROFILES_ACTIVE=prod && mvnw.cmd spring-boot:run
```
Or supply inline:
```powershell
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

## PostgreSQL Local Setup
(Full details also in `POSTGRESQL-SETUP.md`.)
```sql
CREATE DATABASE philldesk_db;
CREATE USER philldesk_user WITH PASSWORD 'secure_password_123';
GRANT ALL PRIVILEGES ON DATABASE philldesk_db TO philldesk_user;
```
Optional Docker:
```powershell
docker run --name philldesk-postgres -e POSTGRES_DB=philldesk_db -e POSTGRES_USER=philldesk_user -e POSTGRES_PASSWORD=secure_password_123 -p 5432:5432 -d postgres:15
```

## Database Initialization Options
1. Let Hibernate generate / update tables (current default: `ddl-auto=update`).
2. For a fresh Postgres schema with sample data: run `schema.sql`, then `data.sql` manually (order matters) or copy contents into a migration tool (Flyway/Liquibase) if added later.

## Authentication & Authorization
- JWT bearer tokens; header: `Authorization: Bearer <token>`
- Obtain token: `POST /api/auth/signin` with JSON `{ "username": "admin", "password": "password" }` (sample users below)
- Roles: ADMIN, PHARMACIST, CUSTOMER (single role per user currently)

Sample test users (after seed / dev startup):
- admin / password (ADMIN)
- pharmacist1 / password (PHARMACIST)
- customer1 / password (CUSTOMER)

`SIGNIN-TESTING-GUIDE.md` contains PowerShell & curl examples.

## Core API Endpoints (Representative)
(Exact list discoverable via controller classes.)
- `POST /api/auth/signin` – login
- `POST /api/auth/signup` – register user
- `GET /api/auth/profile` – current user profile
- `PUT /api/auth/profile` – update profile
- `POST /api/auth/change-password` – password change
- `GET /api/medicines` – list medicines
- `POST /api/prescriptions` – upload prescription metadata/file
- `GET /api/bills` – list bills
- `GET /api/notifications` – user notifications

## File Uploads
- Config property `file.upload.directory=uploads/prescriptions`.
- Ensure folder exists (it's committed). In production mount persistent storage.
- Access base URL built from `file.base.url` + `server.port`.

## JWT Configuration
Defined in `application.properties`:
```
jwt.secret=PhillDeskSecretKeyForJWTTokenGenerationAndValidation2024
jwt.expiration=86400000   # 1 day (ms)
```
For production override via environment variables:
```powershell
$env:JWT_SECRET="<Base64EncodedSecret>"
$env:JWT_EXPIRATION="86400000"
```
(Then reference with `${JWT_SECRET}` after adding keys to properties or use command-line `-Djwt.secret=...`.)

## Building a Jar
```powershell
mvnw.cmd clean package -DskipTests
java -jar target/philldesk-backend-0.0.1-SNAPSHOT.jar
```
With profile:
```powershell
java -jar -Dspring.profiles.active=prod target/philldesk-backend-0.0.1-SNAPSHOT.jar
```

## Environment Variable Summary (suggested)
| Variable | Purpose | Example |
|----------|---------|---------|
| SPRING_PROFILES_ACTIVE | Select profile | prod |
| DATABASE_URL | JDBC URL | jdbc:postgresql://localhost:5432/philldesk_db |
| DATABASE_USERNAME | DB user | philldesk_user |
| DATABASE_PASSWORD | DB password | changeMe! |
| PORT | Server port override | 8080 |
| CONTEXT_PATH | Servlet context path | /api |
| JWT_SECRET | Base64 secret (must be sufficiently long) | (generated) |
| JWT_EXPIRATION | Token validity ms | 86400000 |

## Scheduling
`@EnableScheduling` enabled; pool size configured in prod profile (`spring.task.scheduling.pool.size=2`).

## PDF Generation
Using iText 7 (`kernel`, `layout`, `io`) for invoice / report generation (see service layer for implementations if added).

## Password Storage
Existing sample data may include placeholders (`$2a$10$example`). New users use encoded BCrypt via `PasswordEncoder` bean.

## Development Tips
- If H2 `data.sql` insert conflicts occur, clear `./data/devdb*` files to start fresh.
- Keep secrets out of VCS; use environment variables or a secrets manager.
- Consider adding Flyway for controlled schema migrations before production deployment.

## Troubleshooting
| Issue | Cause | Fix |
|-------|-------|-----|
| 401 Unauthorized | Missing/invalid JWT | Re-authenticate via /api/auth/signin |
| DB connection refused | Postgres not running | Start service / container |
| Table missing in Postgres | Relying on H2 only | Run with prod profile or execute schema.sql |
| Upload fails | Directory missing / permissions | Create and grant write to `uploads/prescriptions` |
| H2 console not loading | Wrong URL | Use `/h2-console` and path `jdbc:h2:file:./data/devdb` |

## Next Steps (Recommended Enhancements)
- Add Flyway or Liquibase for versioned migrations
- Add Swagger/OpenAPI (springdoc-openapi) for interactive docs
- Add Dockerfile & docker-compose (app + Postgres)
- Centralized exception handling & standardized API error format
- Enable HTTPS & production-grade JWT secret management

---
Maintainers: Update this README when endpoints, profiles, or infra steps change.
