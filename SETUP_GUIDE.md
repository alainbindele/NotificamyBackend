# Guida alla Configurazione Clerk per NotifyMe Backend

## 1. Configurazione Clerk

### Passo 1: Creare un'applicazione Clerk
1. Vai su [Clerk Dashboard](https://dashboard.clerk.com/)
2. Clicca su "Create application"
3. Nome: "NotifyMe"
4. Scegli i metodi di autenticazione desiderati (email, Google, GitHub, etc.)
5. Clicca "Create application"

### Passo 2: Ottenere le chiavi API
1. Nel dashboard di Clerk, vai su "API Keys"
2. Copia la "Publishable key" (inizia con `pk_`)
3. Copia la "Secret key" (inizia con `sk_`)

### Passo 3: Configurare i domini (opzionale)
Se necessario, configura i domini consentiti nelle impostazioni dell'applicazione Clerk.

## 2. Configurazione delle Variabili di Ambiente

Crea il file `.env` nella root del progetto con questi valori:

```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://localhost:3306/NotificamyDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USER=your_mysql_username
MYSQL_PASS=your_mysql_password

# Clerk Configuration
CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key
CLERK_SECRET_KEY=sk_test_your_secret_key

# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key

# Server Configuration
SERVER_PORT=8080
```

**Sostituisci:**
- `pk_test_your_publishable_key` con la tua Clerk publishable key
- `sk_test_your_secret_key` con la tua Clerk secret key
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
CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key
CLERK_SECRET_KEY=sk_test_your_secret_key
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
User=ec2-TUser
WorkingDirectory=/home/ec2-TUser/notifyme-backend
ExecStart=/usr/bin/java -jar target/notifyme-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
Environment=DATABASE_URL=jdbc:mysql://localhost:3306/NotificamyDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
Environment=MYSQL_USER=your_mysql_username
Environment=MYSQL_PASS=your_mysql_password
Environment=CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key
Environment=CLERK_SECRET_KEY=sk_test_your_secret_key
Environment=OPENAI_API_KEY=your_openai_api_key
Environment=SERVER_PORT=8080

[Install]
WantedBy=multi-TUser.target
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

### Test con token JWT (dal frontend con Clerk):
```javascript
// Nel tuo frontend React, dopo l'autenticazione Clerk:
import { useAuth } from '@clerk/clerk-react';

const { getToken } = useAuth();
const token = await getToken();

const response = await fetch('https://notificamy.com/api/v1/TUser-info', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

## 6. Troubleshooting

### Problemi comuni:

1. **CORS Error**: Verifica che gli origins siano configurati correttamente nel backend
2. **JWT Invalid**: Controlla che `CLERK_SECRET_KEY` sia corretto
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

## 7. Configurazione Frontend con Clerk (per riferimento)

Il tuo frontend React dovrebbe essere configurato con Clerk così:

```javascript
// App.js
import { ClerkProvider } from '@clerk/clerk-react';

const clerkPubKey = process.env.REACT_APP_CLERK_PUBLISHABLE_KEY;

function App() {
  return (
    <ClerkProvider publishableKey={clerkPubKey}>
      {/* La tua app */}
    </ClerkProvider>
  );
}

// Componente per chiamate API
import { useAuth } from '@clerk/clerk-react';

function ApiComponent() {
  const { getToken } = useAuth();
  
  const callApi = async () => {
    const token = await getToken();
    const response = await fetch('/api/v1/validate-prompt', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        prompt: 'Test prompt',
        email: 'test@example.com'
      })
    });
  };
}
```

### Variabili di ambiente frontend:
```bash
# .env nel frontend
REACT_APP_CLERK_PUBLISHABLE_KEY=pk_test_your_publishable_key
```

## 8. Differenze principali da Auth0

### Vantaggi di Clerk:
1. **Setup più semplice**: Non serve configurare audience o domini complessi
2. **UI components**: Clerk fornisce componenti React pronti all'uso
3. **Gestione utenti**: Dashboard integrata per gestire gli utenti
4. **Pricing**: Spesso più conveniente per piccole/medie applicazioni

### Migrazione da Auth0:
1. **JWT Structure**: I token Clerk hanno una struttura leggermente diversa
2. **User Info**: Le informazioni utente sono accessibili in modo diverso
3. **API Calls**: Non serve più configurare audience, basta il token
4. **Frontend**: Cambia da `@auth0/auth0-react` a `@clerk/clerk-react`

Questo setup dovrebbe permettere al tuo backend di funzionare correttamente con l'autenticazione Clerk!

## 9. Test completo del sistema

### Test del backend:
```bash
# 1. Health check
curl https://notificamy.com/api/v1/health

# 2. Test con token (sostituisci YOUR_CLERK_TOKEN)
curl -X POST https://notificamy.com/api/v1/validate-prompt \
  -H "Authorization: Bearer YOUR_CLERK_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Remind me to call mom tomorrow at 3pm", "email": "test@example.com"}'
```

### Test del frontend:
```javascript
// Verifica che l'autenticazione funzioni
import { useUser, useAuth } from '@clerk/clerk-react';

function TestComponent() {
  const { user } = useUser();
  const { getToken } = useAuth();
  
  const testApi = async () => {
    if (!user) {
      console.log('User not authenticated');
      return;
    }
    
    try {
      const token = await getToken();
      console.log('Token obtained:', token ? 'Success' : 'Failed');
      
      const response = await fetch('/api/v1/user-info', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      const data = await response.json();
      console.log('API Response:', data);
    } catch (error) {
      console.error('API Error:', error);
    }
  };
  
  return (
    <div>
      <p>User: {user?.emailAddresses[0]?.emailAddress}</p>
      <button onClick={testApi}>Test API</button>
    </div>
  );
}
```

Con questa configurazione, il tuo backend NotifyMe dovrebbe funzionare perfettamente con Clerk per l'autenticazione!

## 10. Troubleshooting avanzato

### Problemi con JWT:
```bash
# Verifica il contenuto del token JWT (solo per debug)
echo "YOUR_JWT_TOKEN" | cut -d. -f2 | base64 -d | jq .
```

### Log del backend:
```bash
# Monitora i log per errori di autenticazione
tail -f logs/application.log | grep -i "clerk\|jwt\|auth"
```

### Test delle variabili di ambiente:
```bash
# Verifica che le variabili siano caricate
echo $CLERK_SECRET_KEY | head -c 10
echo $CLERK_PUBLISHABLE_KEY | head -c 10
```

### Verifica connettività Clerk API:
```bash
# Test connessione all'API Clerk
curl -H "Authorization: Bearer $CLERK_SECRET_KEY" \
     https://api.clerk.com/v1/users \
     -w "%{http_code}\n" -o /dev/null -s
```

Se ottieni 200, la connessione con Clerk funziona correttamente!

## 11. Sicurezza in produzione

### Raccomandazioni:
1. **Usa HTTPS**: Sempre in produzione
2. **Ruota le chiavi**: Cambia periodicamente le secret key
3. **Monitora i log**: Tieni traccia dei tentativi di autenticazione
4. **Rate limiting**: Implementa limiti sulle chiamate API
5. **Backup**: Mantieni backup delle configurazioni

### Configurazione sicura:
```bash
# Imposta permessi corretti per il file .env
chmod 600 .env
chown app:app .env

# Usa un service account dedicato
useradd -r -s /bin/false notifyme-app
```

Con questa guida completa, dovresti essere in grado di configurare e far funzionare il backend NotifyMe con Clerk in modo sicuro e affidabile!

## 12. Monitoraggio e metriche

### Metriche da monitorare:
1. **Autenticazioni riuscite/fallite**
2. **Tempo di risposta delle API**
3. **Utilizzo delle risorse**
4. **Errori di validazione JWT**

### Setup monitoring:
```bash
# Esempio con Prometheus metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/health
```

Questo completa la guida per l'integrazione con Clerk. Il sistema dovrebbe ora funzionare correttamente con l'autenticazione Clerk invece di Auth0!

## 13. Configurazione avanzata Clerk

### Personalizzazione del JWT:
Nel dashboard Clerk, puoi personalizzare i claims del JWT:

1. Vai su "JWT Templates" nel dashboard Clerk
2. Crea un nuovo template o modifica quello esistente
3. Aggiungi claims personalizzati se necessario:

```json
{
  "sub": "{{user.id}}",
  "email": "{{user.primary_email_address.email_address}}",
  "name": "{{user.full_name}}",
  "custom_claim": "value"
}
```

### Webhook per sincronizzazione utenti:
Puoi configurare webhook Clerk per sincronizzare automaticamente gli utenti:

```javascript
// Endpoint webhook nel backend (opzionale)
@PostMapping("/api/v1/clerk-webhook")
public ResponseEntity<String> handleClerkWebhook(@RequestBody String payload) {
    // Gestisci eventi Clerk (user.created, user.updated, etc.)
    return ResponseEntity.ok("OK");
}
```

Con queste configurazioni avanzate, avrai un sistema di autenticazione robusto e flessibile con Clerk!

## 14. Migrazione da Auth0 esistente

Se stai migrando da Auth0, ecco i passi principali:

### 1. Backup dei dati utente:
```sql
-- Backup tabella users prima della migrazione
CREATE TABLE users_backup AS SELECT * FROM users;
```

### 2. Mapping degli utenti:
```sql
-- Aggiorna auth_subject con i nuovi ID Clerk
UPDATE users SET auth_subject = 'clerk_user_id' WHERE email = 'user@example.com';
```

### 3. Test parallelo:
- Mantieni temporaneamente entrambi i sistemi
- Testa con un subset di utenti
- Migra gradualmente

### 4. Cleanup finale:
```bash
# Rimuovi le vecchie variabili Auth0
unset AUTH0_DOMAIN
unset AUTH0_AUDIENCE

# Rimuovi le dipendenze Auth0 dal pom.xml (già fatto sopra)
```

La migrazione dovrebbe essere fluida seguendo questi passi!

## 15. Performance e ottimizzazioni

### Cache dei token:
```java
// Opzionale: cache per evitare chiamate ripetute all'API Clerk
@Cacheable("clerk-tokens")
public ClerkUserInfo validateClerkToken(String token) {
    // ... validazione token
}
```

### Connection pooling:
```java
// Ottimizza le chiamate HTTP all'API Clerk
private final HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();
```

### Monitoring specifico:
```java
// Aggiungi metriche personalizzate
@Timed(name = "clerk.token.validation", description = "Time taken to validate Clerk token")
public ClerkUserInfo validateClerkToken(String token) {
    // ... validazione
}
```

Con queste ottimizzazioni, il sistema sarà performante anche sotto carico!

Questo completa la guida completa per l'integrazione di Clerk con il backend NotifyMe. Il sistema dovrebbe ora funzionare perfettamente con l'autenticazione Clerk!

## 16. Configurazione Docker (bonus)

Se usi Docker, ecco un esempio di configurazione:

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

# Variabili di ambiente per Clerk
ENV CLERK_PUBLISHABLE_KEY=""
ENV CLERK_SECRET_KEY=""

COPY target/notifyme-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  notifyme-backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - CLERK_PUBLISHABLE_KEY=${CLERK_PUBLISHABLE_KEY}
      - CLERK_SECRET_KEY=${CLERK_SECRET_KEY}
      - DATABASE_URL=${DATABASE_URL}
      - MYSQL_USER=${MYSQL_USER}
      - MYSQL_PASS=${MYSQL_PASS}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - mysql
  
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=rootpassword
      - MYSQL_DATABASE=NotificamyDB
    ports:
      - "3306:3306"
```

Con Docker, il deployment diventa ancora più semplice!

Questa è la guida completa per integrare Clerk con il backend NotifyMe. Seguendo tutti questi passi, dovresti avere un sistema di autenticazione robusto, sicuro e performante!

## 17. Checklist finale

Prima di andare in produzione, verifica:

- [ ] Clerk application creata e configurata
- [ ] Chiavi API Clerk configurate nel backend
- [ ] Variabili di ambiente impostate correttamente
- [ ] CORS configurato per i domini corretti
- [ ] Database aggiornato e funzionante
- [ ] Frontend configurato con Clerk React SDK
- [ ] Test di autenticazione completati
- [ ] Test delle API con token JWT completati
- [ ] HTTPS configurato in produzione
- [ ] Monitoring e logging attivi
- [ ] Backup del database effettuato

Se tutti i punti sono verificati, il sistema è pronto per la produzione! 🚀

## 18. Supporto e risorse

### Documentazione utile:
- [Clerk Documentation](https://clerk.com/docs)
- [Clerk React SDK](https://clerk.com/docs/references/react/overview)
- [Clerk Backend API](https://clerk.com/docs/references/backend/overview)
- [JWT.io](https://jwt.io/) per debug dei token

### Community e supporto:
- [Clerk Discord](https://clerk.com/discord)
- [Clerk GitHub](https://github.com/clerkinc)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/clerk)

Con queste risorse, dovresti essere in grado di risolvere qualsiasi problema e continuare a migliorare il sistema!

**Fine della guida completa per l'integrazione Clerk con NotifyMe Backend** ✅

Il sistema è ora completamente configurato per funzionare con Clerk invece di Auth0. Tutte le funzionalità dovrebbero continuare a funzionare normalmente, ma con un sistema di autenticazione più moderno e flessibile!

## 19. FAQ (Domande Frequenti)

### Q: Posso usare sia Auth0 che Clerk contemporaneamente?
A: Tecnicamente sì, ma non è raccomandato. È meglio migrare completamente a Clerk per evitare complessità.

### Q: I token Clerk scadono?
A: Sì, i token JWT di Clerk hanno una scadenza. Il frontend dovrebbe gestire automaticamente il refresh.

### Q: Come gestisco i ruoli utente con Clerk?
A: Clerk supporta ruoli e permessi. Puoi configurarli nel dashboard e accedervi tramite i claims JWT.

### Q: Clerk funziona con applicazioni mobile?
A: Sì, Clerk ha SDK per React Native, iOS e Android.

### Q: Come faccio il backup degli utenti Clerk?
A: Puoi esportare gli utenti tramite l'API Clerk o il dashboard.

### Q: Clerk supporta l'autenticazione multi-fattore?
A: Sì, Clerk supporta SMS, TOTP e altri metodi MFA.

### Q: Posso personalizzare le pagine di login?
A: Sì, Clerk permette personalizzazione completa delle UI di autenticazione.

### Q: Come gestisco gli errori di autenticazione?
A: Il backend restituisce errori HTTP 401 con messaggi JSON dettagliati.

### Q: Clerk è GDPR compliant?
A: Sì, Clerk è conforme a GDPR e altre normative sulla privacy.

### Q: Come monitoro l'utilizzo di Clerk?
A: Il dashboard Clerk fornisce analytics dettagliati sull'utilizzo.

Queste FAQ dovrebbero coprire i dubbi più comuni sull'integrazione Clerk!

---

**🎉 Congratulazioni! Hai completato con successo l'integrazione di Clerk con il backend NotifyMe!**

Il sistema è ora pronto per gestire l'autenticazione tramite Clerk in modo sicuro e affidabile. Buon lavoro! 🚀
    >
      {children}
    </Auth0Provider>
  );
};
```

Questo setup dovrebbe permettere al tuo backend di funzionare correttamente con l'autenticazione Auth0!