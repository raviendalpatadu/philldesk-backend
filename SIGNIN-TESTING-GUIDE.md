# PhillDesk Sign-In Testing Guide
# ================================

## Method 1: PowerShell (Command Line)

### Basic Sign-in Test
```powershell
$body = '{"username":"admin","password":"password"}'
$headers = @{"Content-Type"="application/json"}
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/signin" -Method Post -Headers $headers -Body $body
$result = $response.Content | ConvertFrom-Json
Write-Host "Token: $($result.accessToken)"
Write-Host "Username: $($result.username)"
Write-Host "Roles: $($result.roles)"
```

### Using the Token for Authenticated Requests
```powershell
$authHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $($result.accessToken)"
}
$medicines = Invoke-WebRequest -Uri "http://localhost:8080/api/medicines" -Method Get -Headers $authHeaders
```

## Method 2: curl (if available)
```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

## Method 3: Postman (Recommended for GUI)

### Setup:
1. Create new POST request
2. URL: http://localhost:8080/api/auth/signin
3. Headers: Content-Type: application/json
4. Body (raw JSON):
```json
{
    "username": "admin",
    "password": "password"
}
```

### Expected Response:
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "username": "admin",
    "email": "admin@philldesk.com",
    "roles": ["ADMIN"]
}
```

## Available Test Users:
- Username: admin, Password: password (ADMIN role)
- Username: pharmacist1, Password: password (PHARMACIST role)  
- Username: customer1, Password: password (CUSTOMER role)

## Testing Authenticated Endpoints:
After getting a token, add it to Authorization header:
- Authorization: Bearer YOUR_JWT_TOKEN

### Available Endpoints:
- GET /api/medicines - List all medicines
- GET /api/users/profile - Get user profile
- POST /api/prescriptions - Create prescription
- GET /api/bills - List bills
- And more...

## Troubleshooting:
- 401 Unauthorized: Wrong credentials
- 500 Internal Server Error: Check server logs
- Connection refused: Server not running
- CORS issues: Enable CORS for frontend domain
