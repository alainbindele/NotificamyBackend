# Configurazione Auth0 per NotifyMe

## PROBLEMA ATTUALE
L'errore "Service not found: https://notificamy.com" indica che Auth0 non trova gli endpoint OAuth2 corretti.

## CAUSA
Il tuo backend Spring Boot è configurato come **JWT Resource Server** (per validare token), ma Auth0 si aspetta un **OAuth2 Client** (per gestire il flusso di login).

## SOLUZIONE

### 1. Configurazione Auth0 Dashboard

#### A. Applicazione Frontend (Single Page Application)
1. Vai su [Auth0 Dashboard](https://manage.auth0.com/)
2. Applications → Create Application
3. Nome: "NotifyMe Frontend"
4. Tipo: **Single Page Web Applications**
5. Nelle Settings:

**Allowed Callback URLs:**
```
https://notificamy.com,
https://notificamy.com/callback,
https://www.notificamy.com,
https://www.notificamy.com/callback
```

**Allowed Logout URLs:**
```
https://notificamy.com,
https://www.notificamy.com
```

**Allowed Web Origins:**
```
https://notificamy.com,
https://www.notificamy.com
```

**Allowed Origins (CORS):**
```
https://notificamy.com,
https://www.notificamy.com
```

#### B. API Auth0
1. APIs → Create API
2. Nome: "NotifyMe API"
3. Identifier: `https://notificamy.com/api`
4. Signing Algorithm: RS256

### 2. Configurazione Frontend React

Il tuo frontend deve essere configurato così:

```javascript
// In App.js o main.jsx
import { Auth0Provider } from '@auth0/auth0-react';

function App() {
  return (
    <Auth0Provider
      domain="TUO-DOMINIO.auth0.com"
      clientId="TUO-CLIENT-ID-FRONTEND"
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: 'https://notificamy.com/api'
      }}
    >
      {/* Il tuo app */}
    </Auth0Provider>
  );
}
```

```javascript
// Per chiamare le API
import { useAuth0 } from '@auth0/auth0-react';

function ApiCall() {
  const { getAccessTokenSilently } = useAuth0();
  
  const callApi = async () => {
    try {
      const token = await getAccessTokenSilently({
        audience: 'https://notificamy.com/api'
      });
      
      const response = await fetch('https://notificamy.com/api/v1/validate-prompt', {
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
      
      const data = await response.json();
      console.log(data);
    } catch (error) {
      console.error('Error:', error);
    }
  };
}
```

### 3. Configurazione Apache/Nginx

Se usi Apache, assicurati che il reverse proxy sia configurato correttamente:

```apache
<VirtualHost *:443>
    ServerName notificamy.com
    
    # Frontend (React build)
    DocumentRoot /var/www/notificamy
    
    # API Backend
    ProxyPass /api/ http://localhost:8080/api/
    ProxyPassReverse /api/ http://localhost:8080/api/
    
    # Headers per HTTPS
    ProxyPreserveHost On
    ProxyAddHeaders On
    
    # SSL config...
</VirtualHost>
```

### 4. Variabili di Ambiente (.env)

Aggiorna il file .env con i valori reali:

```bash
# Sostituisci con i tuoi valori reali da Auth0 Dashboard
AUTH0_DOMAIN=tuo-dominio.auth0.com
AUTH0_AUDIENCE=https://notificamy.com/api

# Database
DATABASE_URL=jdbc:mysql://localhost:3306/NotificamyDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
MYSQL_USER=tuo_username
MYSQL_PASS=tua_password

# OpenAI
OPENAI_API_KEY=tua_chiave_openai

SERVER_PORT=8080
```

### 5. Test della Configurazione

1. **Test Health Endpoint:**
```bash
curl https://notificamy.com/api/v1/health
```

2. **Test dal Frontend:**
- Il login dovrebbe reindirizzare a Auth0
- Dopo il login, dovresti tornare al tuo frontend
- Le chiamate API dovrebbero funzionare con il token JWT

### 6. Flusso Completo

1. **Utente clicca "Login"** → Reindirizzato ad Auth0
2. **Auth0 autentica** → Reindirizza a `https://notificamy.com`
3. **Frontend riceve il token** → Può chiamare le API
4. **Backend valida il JWT** → Risponde con i dati

## IMPORTANTE

Il backend Spring Boot NON gestisce il flusso di login OAuth2. Questo è gestito completamente dal frontend React + Auth0. Il backend si limita a:

1. **Validare i JWT token** ricevuti nelle richieste API
2. **Estrarre informazioni utente** dal token
3. **Autorizzare le richieste** basandosi sul token

Questo è il pattern corretto per applicazioni SPA (Single Page Application) con API backend separate.