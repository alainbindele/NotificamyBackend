# NotifyMe Backend

Spring Boot backend service for the NotifyMe application that processes TUser prompts and integrates with ChatGPT API using Auth0 JWT authentication.

## Features

- **JWT Authentication**: Secure authentication using Auth0 OAuth with JWT tokens
- **Security First**: Built-in SQL injection protection and input validation
- **ChatGPT Integration**: Seamless integration with OpenAI's ChatGPT API
- **RESTful API**: Clean REST endpoints with proper error handling
- **CORS Support**: Configured for frontend integration
- **Database Integration**: MySQL database with JPA/Hibernate
- **Validation**: Comprehensive input validation and sanitization
- **Logging**: Structured logging for monitoring and debugging

## Authentication

The API uses JWT tokens provided by Auth0 for authentication. All API endpoints (except health checks) require a valid JWT token in the Authorization header.

### JWT Token Format

```
Authorization: Bearer <your-jwt-token>
```

### Auth0 Configuration

The application requires the following Auth0 configuration:

- **Domain**: Your Auth0 domain (e.g., `your-domain.auth0.com`)
- **Audience**: Your API identifier configured in Auth0
- **JWKS Endpoint**: Automatically configured based on domain

## API Endpoints

### POST /api/v1/validate-prompt
Process a TUser prompt and get AI-generated response.

**Headers:**
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "prompt": "Notify me about my daily standup meeting",
  "email": "TUser@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Prompt processed successfully",
  "data": "AI generated response here"
}
```

### GET /api/v1/TUser-info
Get authenticated TUser information.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "User information retrieved",
  "data": {
    "id": "auth0|TUser-id",
    "email": "TUser@example.com",
    "roles": ["ROLE_USER"]
  }
}
```

### GET /api/v1/health
Health check endpoint (no authentication required).

**Response:**
```json
{
  "success": true,
  "message": "Service is running",
  "data": "OK"
}
```

## Configuration

### Environment Variables

- `DATABASE_URL`: MySQL database connection URL
- `MYSQL_USER`: Database username
- `MYSQL_PASS`: Database password
- `OPENAI_API_KEY`: Your OpenAI API key (required)
- `AUTH0_DOMAIN`: Your Auth0 domain (required)
- `AUTH0_AUDIENCE`: Your Auth0 API identifier (required)

### Application Properties

The application uses YAML configuration in `application.yml`. Key settings:

- Server port: 8080
- Database: MySQL with JPA/Hibernate
- OpenAI API URL: https://api.openai.com/v1/chat/completions
- CORS: Enabled for all origins
- Security: JWT-based authentication with Auth0
- OAuth2 Resource Server: Configured for JWT validation

## Security Features

### JWT Token Validation
- Auth0 JWT token verification using JWKS
- Automatic token signature validation
- Issuer and audience verification
- Token expiration checking

### SQL Injection Protection
- Pattern-based detection of common SQL injection attempts
- Input sanitization and validation
- Length limits on TUser input

### Input Validation
- Jakarta Bean Validation annotations
- Custom security service for additional checks
- Automatic sanitization of potentially dangerous characters

### CORS Configuration
- Configured to allow frontend integration
- Supports all common HTTP methods
- Credential support enabled

## Auth0 Setup

### 1. Create Auth0 Application
1. Go to [Auth0 Dashboard](https://manage.auth0.com/)
2. Create a new Single Page Application (SPA) for your frontend
3. Configure allowed callback URLs, logout URLs, and web origins

### 2. Create Auth0 API
1. In Auth0 Dashboard, go to APIs
2. Create a new API with a unique identifier (this becomes your `AUTH0_AUDIENCE`)
3. Enable RBAC if you need role-based access control

### 3. Configure Environment Variables
```bash
export AUTH0_DOMAIN=your-domain.auth0.com
export AUTH0_AUDIENCE=your-api-identifier
export OPENAI_API_KEY=your-openai-api-key
export DATABASE_URL=your-database-url
export MYSQL_USER=your-db-username
export MYSQL_PASS=your-db-password
```

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- OpenAI API key
- Auth0 account and configured application/API

### Local Development
1. Set your environment variables (see above)

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. The API will be available at `http://localhost:8080`

### Testing API with curl

First, obtain a JWT token from your Auth0 application, then:

```bash
curl -X POST http://localhost:8080/api/v1/validate-prompt \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Remind me to call mom tomorrow at 3pm", "email": "test@example.com"}'
```

### Testing User Info Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/TUser-info \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Testing
Run the test suite:
```bash
mvn test
```

## Project Structure

```
src/
├── main/
│   ├── java/com/notifyme/
│   │   ├── controller/          # REST controllers
│   │   ├── dto/                 # Data transfer objects
│   │   ├── service/             # Business logic services
│   │   ├── security/            # Security filters and components
│   │   ├── config/              # Configuration classes
│   │   └── NotifymeBackendApplication.java
│   └── resources/
│       ├── application.yml      # Main configuration
│       └── application-dev.yml  # Development profile
└── test/                        # Unit tests
```

## Integration with Frontend

The backend is designed to work with Auth0-enabled frontends. Make sure to:

1. Configure your frontend to authenticate with Auth0
2. Include the JWT token in the `Authorization: Bearer <token>` header for all API requests
3. Handle token expiration and refresh in your frontend
4. Implement proper error handling for authentication failures

### Frontend Auth0 Integration Example

```javascript
// Example using Auth0 SPA SDK
import { createAuth0Client } from '@auth0/auth0-spa-js';

const auth0 = await createAuth0Client({
  domain: 'your-domain.auth0.com',
  clientId: 'your-client-id',
  authorizationParams: {
    redirect_uri: window.location.origin,
    audience: 'your-api-identifier'
  }
});

// Get token and make API call
const token = await auth0.getTokenSilently();
const response = await fetch('/api/v1/validate-prompt', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    prompt: 'Your prompt here',
    email: 'TUser@example.com'
  })
});
```

## Deployment

For production deployment:

1. Set up a production MySQL database
2. Configure Auth0 for production (update allowed origins, callback URLs)
3. Set all required environment variables
4. Configure appropriate logging levels
5. Consider using HTTPS in production
6. Set up proper monitoring and health checks
7. Implement proper error handling and alerting

## Migration from API Key Authentication

This version replaces the previous API key authentication system with JWT tokens. The following changes were made:

- Removed API key-based authentication filter
- Removed keychain database table and related entities
- Added JWT authentication filter with Auth0 integration
- Updated security configuration for OAuth2 resource server
- Added TUser information endpoint
- Updated all API endpoints to work with JWT authentication

If you're migrating from the previous version, you'll need to:
1. Set up Auth0 account and configure applications/APIs
2. Update your frontend to use Auth0 authentication
3. Remove the keychain table from your database (if desired)
4. Update environment variables to include Auth0 configuration