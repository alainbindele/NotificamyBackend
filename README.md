# NotifyMe Backend

Spring Boot backend service for the NotifyMe application that processes user prompts and integrates with ChatGPT API.

## Features

- **Security First**: Built-in SQL injection protection and input validation
- **ChatGPT Integration**: Seamless integration with OpenAI's ChatGPT API
- **RESTful API**: Clean REST endpoints with proper error handling
- **CORS Support**: Configured for frontend integration
- **Validation**: Comprehensive input validation and sanitization
- **Logging**: Structured logging for monitoring and debugging

## API Endpoints

### POST /api/v1/prompt
Process a user prompt and get AI-generated response.

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
Health check endpoint.

## Configuration

### Environment Variables

- `OPENAI_API_KEY`: Your OpenAI API key (required)

### Application Properties

The application uses YAML configuration in `application.yml`. Key settings:

- Server port: 8080
- OpenAI API URL: https://api.openai.com/v1/chat/completions
- CORS: Enabled for all origins
- Security: CSRF disabled for API usage

## Security Features

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
- OpenAI API key

### Local Development
1. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. The API will be available at `http://localhost:8080`

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
2. Handle the API response format in your frontend code
3. Implement proper error handling for validation failures

## Deployment

For production deployment:

1. Set the `OPENAI_API_KEY` environment variable
2. Configure appropriate logging levels
3. Consider using HTTPS in production
4. Set up proper monitoring and health checks