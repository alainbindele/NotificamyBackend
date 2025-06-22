# Guida alla Configurazione Auth0 per NotifyMe Backend

## 1. Configurazione Auth0

### Passo 1: Creare un'applicazione Auth0
1. Vai su [Auth0 Dashboard](https://manage.auth0.com/)
2. Clicca su "Applications" nel menu laterale
3. Clicca su "Create Application"
4. Nome: "NotifyMe Frontend"
5. Tipo: "Single Page Web Applications"
6. Clicca "Create"

### Passo 2: Configurare l'applicazione
Nelle impostazioni dell'applicazione:

**Allowed Callback URLs:**
```
https://notificamy.com/callback,
https://www.notificamy.com/callback,
http://localhost:3000/callback,
http://localhost:5173/callback
```

**Allowed Logout URLs:**
```
https://notificamy.com,
https://www.notificamy.com,
http://localhost:3000,
http://localhost:5173
```

**Allowed Web Origins:**
```
https://notificamy.com,
https://www.notificamy.com,
http://localhost:3000,
http://localhost:5173
```

**Allowed Origins (CORS):**
```
https://notificamy.com,
https://www.notificamy.com,
http://localhost:3000,
http://localhost:5173
```

### Passo 3: Creare un'API Auth0
1. Vai su "APIs" nel menu laterale
2. Clicca "Create API"
3. Nome: "NotifyMe API"
4. Identifier: `https://notificamy.com/api`
5. Signing Algorithm: RS256
6. Clicca "Create"

### Passo 4: Configurare l'API
Nelle impostazioni dell'API:
- Abilita "Enable RBAC" se vuoi gestire ruoli
- Abilita "Add Permissions in the Access Token" se necessario

## 2. Configurazione delle Variabili di Ambiente

Crea il file `.env` nella root del progetto con questi valori:

```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://localhost:3306/NotificamyDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USER=your_mysql_username
MYSQL_PASS=your_mysql_password

# Auth0 Configuration
AUTH0_DOMAIN=your-auth0-domain.auth0.com
AUTH0_AUDIENCE=https://notificamy.com/api

# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key

# Server Configuration
SERVER_PORT=8080
```

**Sostituisci:**
- `your-auth0-domain.auth0.com` con il tuo dominio Auth0 (es: `notificamy.auth0.com`)
- `your_mysql_username` e `your_mysql_password` con le credenziali del database
- `your_openai_api_key` con la tua chiave API OpenAI

## 3. Configurazione del Server EC2

### Passo 1: Installare Java 17
```bash
sudo yum update -y
sudo yum install -y java-17-amazon-corretto-devel
```

### Passo 2: Installare Maven
```bash
sudo yum install -y maven
```

### Passo 3: Configurare le variabili di ambiente
```bash
# Crea il file delle variabili di ambiente
sudo nano /etc/environment

# Aggiungi le variabili:
DATABASE_URL=jdbc:mysql://localhost:3306/NotificamyDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USER=your_mysql_username
MYSQL_PASS=your_mysql_password
AUTH0_DOMAIN=your-auth0-domain.auth0.com
AUTH0_AUDIENCE=https://notificamy.com/api
OPENAI_API_KEY=your_openai_api_key
SERVER_PORT=8080
```

### Passo 4: Configurare il firewall
```bash
# Apri la porta 8080
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

### Passo 5: Configurare il reverse proxy (Nginx)
```bash
# Installa Nginx
sudo yum install -y nginx

# Configura Nginx
sudo nano /etc/nginx/conf.d/notificamy.conf
```

Contenuto del file di configurazione Nginx:
```nginx
server {
    listen 80;
    server_name notificamy.com www.notificamy.com;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name notificamy.com www.notificamy.com;
    
    # SSL configuration (configura i tuoi certificati SSL)
    ssl_certificate /path/to/your/certificate.crt;
    ssl_certificate_key /path/to/your/private.key;
    
    # Frontend (React app)
    location / {
        root /var/www/notificamy;
        try_files $uri $uri/ /index.html;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # CORS headers
        add_header 'Access-Control-Allow-Origin' 'https://notificamy.com' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
        
        if ($request_method = 'OPTIONS') {
            return 204;
        }
    }
}
```

## 4. Avvio dell'Applicazione

### Sviluppo locale:
```bash
# Carica le variabili di ambiente
source .env

# Avvia l'applicazione
mvn spring-boot:run
```

### Produzione su EC2:
```bash
# Build dell'applicazione
mvn clean package -DskipTests

# Avvia l'applicazione
java -jar target/notifyme-backend-0.0.1-SNAPSHOT.jar
```

### Creare un servizio systemd (raccomandato per produzione):
```bash
sudo nano /etc/systemd/system/notifyme-backend.service
```

Contenuto del file:
```ini
[Unit]
Description=NotifyMe Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user/notifyme-backend
ExecStart=/usr/bin/java -jar target/notifyme-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
Environment=DATABASE_URL=jdbc:mysql://localhost:3306/NotificamyDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
Environment=MYSQL_USER=your_mysql_username
Environment=MYSQL_PASS=your_mysql_password
Environment=AUTH0_DOMAIN=your-auth0-domain.auth0.com
Environment=AUTH0_AUDIENCE=https://notificamy.com/api
Environment=OPENAI_API_KEY=your_openai_api_key
Environment=SERVER_PORT=8080

[Install]
WantedBy=multi-user.target
```

Avvia il servizio:
```bash
sudo systemctl daemon-reload
sudo systemctl enable notifyme-backend
sudo systemctl start notifyme-backend
sudo systemctl status notifyme-backend
```

## 5. Test della Configurazione

### Test dell'endpoint di health:
```bash
curl https://notificamy.com/api/v1/health
```

### Test con token JWT (dal frontend):
```javascript
// Nel tuo frontend React, dopo l'autenticazione Auth0:
const token = await getAccessTokenSilently({
  audience: 'https://notificamy.com/api'
});

const response = await fetch('https://notificamy.com/api/v1/user-info', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

## 6. Troubleshooting

### Problemi comuni:

1. **CORS Error**: Verifica che gli origins siano configurati correttamente in Auth0 e nel backend
2. **JWT Invalid**: Controlla che `AUTH0_DOMAIN` e `AUTH0_AUDIENCE` siano corretti
3. **Database Connection**: Verifica le credenziali del database e la connettività
4. **Port 8080 not accessible**: Controlla il firewall e le security groups di AWS

### Log utili:
```bash
# Visualizza i log dell'applicazione
sudo journalctl -u notifyme-backend -f

# Visualizza i log di Nginx
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log
```

## 7. Configurazione Frontend (per riferimento)

Il tuo frontend React dovrebbe essere configurato così:

```javascript
// auth0-provider.js
import { Auth0Provider } from '@auth0/auth0-react';

const Auth0ProviderWithHistory = ({ children }) => {
  return (
    <Auth0Provider
      domain="your-auth0-domain.auth0.com"
      clientId="your-auth0-client-id"
      authorizationParams={{
        redirect_uri: window.location.origin + '/callback',
        audience: 'https://notificamy.com/api'
      }}
    >
      {children}
    </Auth0Provider>
  );
};
```

Questo setup dovrebbe permettere al tuo backend di funzionare correttamente con l'autenticazione Auth0!