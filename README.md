# NotifyMe Backend

Spring Boot backend service for the NotifyMe application that processes user prompts and integrates with ChatGPT API using Auth0 JWT authentication.

**Note**: This backend focuses only on prompt validation and query management. Batch processing and notifications are handled by separate microservices.

## Features

- **JWT Authentication**: Secure authentication using Auth0 OAuth with JWT tokens
- **Security First**: Built-in SQL injection protection and input validation
- **ChatGPT Integration**: Seamless integration with OpenAI's ChatGPT API
- **RESTful API**: Clean REST endpoints with proper error handling
- **CORS Support**: Configured for frontend integration
- **Database Integration**: MySQL database with JPA/Hibernate
- **Validation**: Comprehensive input validation and sanitization
- **Logging**: Structured logging for monitoring and debugging
- **Query Management**: Complete CRUD operations for notification queries
- **Multi-language Support**: Works with prompts in any natural language

## Authentication

The API uses JWT tokens provided by Clerk for authentication. All API endpoints (except health checks) require a valid JWT token in the Authorization header.

### JWT Token Format

```
Authorization: Bearer <your-jwt-token>
```

### Clerk Configuration

The application requires the following Clerk configuration:

- **Publishable Key**: Your Clerk publishable key (starts with `pk_`)
- **Secret Key**: Your Clerk secret key (starts with `sk_`)
- **API Verification**: Automatic JWT validation with Clerk's infrastructure

## API Endpoints

### POST /api/v1/validate-prompt
Process a user prompt and get AI-generated validation response.

**Headers:**
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "prompt": "Notify me about my daily standup meeting",
  "email": "user@example.com",
  "channels": ["email", "discord"],
  "channelConfigs": {
    "email": "user@example.com",
    "discord": "https://discord.com/api/webhooks/..."
  },
  "timezone": "Europe/Rome"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Prompt processed successfully",
  "data": "AI generated validation response here"
}
```

### GET /api/v1/TUser-info
Get authenticated user information.

### GET /api/v1/user/profile
Get complete user profile with notification channels (masked).

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "User profile retrieved successfully",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "John Doe",
    "createdAt": "2025-01-20T10:00:00",
    "discordWebhook": "https://dis***...xyz",
    "slackWebhook": "https://hoo***...abc",
    "phone": "+39***...789"
  }
}
```

### PUT /api/v1/user/profile
Update user profile (display name and/or email).

**Headers:**
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "displayName": "John Smith",
  "email": "john.smith@example.com"
}
```

### PUT /api/v1/user/notification-channels
Update notification channel configurations.

**Headers:**
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "discord": "https://discord.com/api/webhooks/...",
  "slack": "https://hooks.slack.com/services/...",
  "whatsapp": "+393123456789"
}
```

### DELETE /api/v1/user/account
Delete user account and all associated data.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "User account deleted successfully",
  "data": "OK"
}
```

### GET /api/v1/user/statistics
Get user statistics and usage information.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "User statistics retrieved successfully",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "displayName": "John Doe",
    "memberSince": "2025-01-20T10:00:00",
    "daysSinceRegistration": 5,
    "configuredChannels": 3,
    "totalQueries": 10,
    "activeQueries": 7,
    "cronQueries": 5,
    "specificQueries": 3,
    "checkQueries": 2
  }
}
```

### GET /api/v1/queries
Get all queries for the authenticated user.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Queries retrieved successfully",
  "data": [
    {
      "id": 1,
      "prompt": "Notify me every day at 9 AM",
      "isValid": true,
      "cron": true,
      "cronParams": "0 9 * * *",
      "summaryText": "Daily notification at 9 AM",
      "createdAt": "2025-01-20T10:00:00"
    }
  ]
}
```

### GET /api/v1/queries/active
Get only active queries for the authenticated user.

### GET /api/v1/queries/statistics
Get query statistics for the authenticated user.

### PUT /api/v1/queries/{queryId}/close
Close a specific query.

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
- `CLERK_PUBLISHABLE_KEY`: Your Clerk publishable key (optional for backend)
- `CLERK_SECRET_KEY`: Your Clerk secret key (required)
- `OPENAI_MODEL`: OpenAI model to use (default: gpt-4o-mini)

### Application Properties

The application uses YAML configuration in `application.yml`. Key settings:

- Server port: 8080
- Database: MySQL with JPA/Hibernate
- OpenAI API URL: https://api.openai.com/v1/chat/completions
- CORS: Enabled for all origins
- Security: JWT-based authentication with Clerk
- OAuth2 Resource Server: Configured for JWT validation
- Scheduling: Disabled (handled by external services)

## Security Features

### JWT Token Validation
- Clerk JWT token verification
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
- Multi-channel notification configuration validation

### CORS Configuration
- Configured to allow frontend integration
- Supports all common HTTP methods
- Credential support enabled

## Clerk Setup

### 1. Create Clerk Application
1. Go to [Clerk Dashboard](https://dashboard.clerk.com/)
2. Create a new application
3. Choose your preferred authentication methods (email, social providers, etc.)

### 2. Get API Keys
1. In Clerk Dashboard, go to API Keys
2. Copy your Publishable Key (starts with `pk_`)
3. Copy your Secret Key (starts with `sk_`)

### 3. Configure Environment Variables
```bash
export CLERK_PUBLISHABLE_KEY=pk_test_...
export CLERK_SECRET_KEY=sk_test_...
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

First, obtain a JWT token from your Clerk application, then:

```bash
curl -X POST http://localhost:8080/api/v1/validate-prompt \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Remind me to call mom tomorrow at 3pm", "email": "test@example.com", "timezone": "Europe/Rome"}'
```

### Testing Queries Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/queries \
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
│   │   ├── entity/              # JPA entities
│   │   ├── repository/          # Data repositories
│   │   └── NotifymeBackendApplication.java
│   └── resources/
│       ├── application.yml      # Main configuration
│       └── application-dev.yml  # Development profile
└── test/                        # Unit tests
```

## Integration with Frontend

The backend is designed to work with Clerk-enabled frontends. Make sure to:

1. Configure your frontend to authenticate with Clerk
2. Include the JWT token in the `Authorization: Bearer <token>` header for all API requests
3. Handle token expiration and refresh in your frontend
4. Implement proper error handling for authentication failures
5. Use the query management endpoints to display user's notification configurations

### Frontend Clerk Integration Example

```javascript
// Example using Clerk React SDK
import { useAuth } from '@clerk/clerk-react';

function ApiCall() {
  const { getToken } = useAuth();

  const makeApiCall = async () => {
    const token = await getToken();
    const response = await fetch('/api/v1/validate-prompt', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        prompt: 'Your prompt here',
        email: 'user@example.com',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
      })
    });
  };
}
```

## Architecture Notes

This backend service is designed as part of a microservices architecture:

- **This Service**: Handles prompt validation, query management, and user data
- **Batch Service** (External): Processes scheduled queries and executes notifications
- **Notification Service** (External): Sends notifications via various channels (email, Discord, Slack, WhatsApp)

The separation allows for:
- Better scalability and resource allocation
- Independent deployment and updates
- Specialized optimization for each concern
- Fault isolation between services

## Deployment

For production deployment:

1. Set up a production MySQL database
2. Configure Clerk for production (update allowed origins in Clerk dashboard)
3. Set all required environment variables
4. Configure appropriate logging levels
5. Consider using HTTPS in production
6. Set up proper monitoring and health checks
7. Implement proper error handling and alerting
8. Configure external batch and notification services
9. Set up service discovery if using microservices

## Database Schema

The application uses the following main tables:

- **users**: User accounts and notification channel configurations
- **queries**: Notification queries with full ChatGPT validation data
- **executions**: Historical execution records (managed by external batch service)
- **notifications**: Sent notification history (managed by external notification service)

See the migration files in `supabase/migrations/` for the complete schema definition.