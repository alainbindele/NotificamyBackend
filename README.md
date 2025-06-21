# NotifyMe Backend

Spring Boot backend service for the NotifyMe application that processes user prompts and integrates with ChatGPT API.

## Features

- **API Key Authentication**: Secure authentication using database-stored API keys
- **Security First**: Built-in SQL injection protection and input validation
- **ChatGPT Integration**: Seamless integration with OpenAI's ChatGPT API
- **RESTful API**: Clean REST endpoints with proper error handling
- **CORS Support**: Configured for frontend integration
- **Database Integration**: MySQL database with JPA/Hibernate
- **Validation**: Comprehensive input validation and sanitization
- **Logging**: Structured logging for monitoring and debugging

## Authentication

All API endpoints (except health checks) require authentication via API key. The API key must be provided in the `api-key` header.

### API Key Management

API keys are stored in the `keychain` table with the following structure:

```sql
CREATE TABLE `keychain` (
  `id` int UNSIGNED NOT NULL,
  `apikey` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `alias` varchar(100) DEFAULT NULL,
  `expired` tinyint NOT NULL DEFAULT '0',
  `disabled` tinyint DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

- `apikey`: The actual API key string
- `alias`: Optional human-readable name for the key
- `expired`: Set to 1 to mark the key as expired
- `disabled`: Set to 1 to temporarily disable the key

Only API keys with `expired = 0` and `disabled = 0` are considered valid.

## API Endpoints

### POST /api/v1/validate-prompt
Process a user prompt and get AI-generated response.

**Headers:**
```
api-key: your-api-key-here
Content-Type: application/json
```

**Request Body:**
```json
{
  "prompt": "Notify me about my daily standup meeting",
  "email": "user@example.com"
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

- `DATABASE_URL`: MySQL database connection URL (default: jdbc:mysql://localhost:3306/notifyme?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true)
- `DATABASE_USERNAME`: Database username (default: root)
- `DATABASE_PASSWORD`: Database password (default: password)
- `OPENAI_API_KEY`: Your OpenAI API key (required)

### Application Properties

The application uses YAML configuration in `application.yml`. Key settings:

- Server port: 8080
- Database: MySQL with JPA/Hibernate
- OpenAI API URL: https://api.openai.com/v1/chat/completions
- CORS: Enabled for all origins
- Security: API key-based authentication

## Security Features

### API Key Authentication
- Database-backed API key validation
- Support for key expiration and disabling
- Secure header-based authentication
- Automatic rejection of invalid/expired keys

### SQL Injection Protection
- Pattern-based detection of common SQL injection attempts
- Input sanitization and validation
- Length limits on user input

### Input Validation
- Jakarta Bean Validation annotations
- Custom security service for additional checks
- Automatic sanitization of potentially dangerous characters

### CORS Configuration
- Configured to allow frontend integration
- Supports all common HTTP methods
- Credential support enabled

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- OpenAI API key

### Database Setup
1. Create a MySQL database named `notifyme`
2. Create the `keychain` table using the SQL provided above
3. Insert at least one valid API key:
   ```sql
   INSERT INTO keychain (apikey, alias, expired, disabled) 
   VALUES ('your-secure-api-key-here', 'development-key', 0, 0);
   ```

### Local Development
1. Set your environment variables:
   ```bash
   export DATABASE_URL=jdbc:mysql://localhost:3306/notifyme?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   export DATABASE_USERNAME=your-db-username
   export DATABASE_PASSWORD=your-db-password
   export OPENAI_API_KEY=your-openai-api-key
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. The API will be available at `http://localhost:8080`

### Testing API with curl
```bash
curl -X POST http://localhost:8080/api/v1/validate-prompt \
  -H "api-key: your-secure-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Remind me to call mom tomorrow at 3pm", "email": "test@example.com"}'
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
│   │   ├── entity/              # JPA entities
│   │   ├── repository/          # Data repositories
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

The backend is designed to work with the NotifyMe React frontend. Make sure to:

1. Update the frontend API base URL to point to this backend
2. Include the `api-key` header in all API requests
3. Handle the API response format in your frontend code
4. Implement proper error handling for authentication and validation failures

## Deployment

For production deployment:

1. Set up a production MySQL database
2. Create and configure API keys in the keychain table
3. Set all required environment variables
4. Configure appropriate logging levels
5. Consider using HTTPS in production
6. Set up proper monitoring and health checks
7. Implement API key rotation strategy