# Prompt per Sviluppo Frontend NotifyMe

Sei un esperto sviluppatore frontend che deve creare un'applicazione React moderna per il sistema NotifyMe. Il backend è già sviluppato e funzionante con autenticazione Auth0 e API REST complete.

## 🎯 Obiettivo del Progetto

NotifyMe è un sistema di notifiche intelligenti che permette agli utenti di creare notifiche personalizzate usando linguaggio naturale. Gli utenti possono dire cose come:
- "Notificami ogni giorno alle 9 sulle notizie"
- "Ricordami il 21 gennaio di chiamare mamma"
- "Avvisami se il prezzo di Bitcoin scende sotto i 50.000$"

Il sistema usa ChatGPT per interpretare e validare i prompt degli utenti.

## 🔐 Autenticazione

Il sistema usa **Auth0** per l'autenticazione. Configurazione richiesta:

```javascript
// Auth0 Provider Setup
import { Auth0Provider } from '@auth0/auth0-react';

const auth0Config = {
  domain: "your-domain.auth0.com",
  clientId: "your-client-id",
  authorizationParams: {
    redirect_uri: window.location.origin + '/callback',
    audience: 'https://notificamy.com/api'
  }
};

// Per ottenere il token JWT per le API calls:
const { getAccessTokenSilently } = useAuth0();
const token = await getAccessTokenSilently({
  audience: 'https://notificamy.com/api'
});
```

## 🌐 Base URL e Headers

- **Base URL**: `https://notificamy.com/api/v1` (produzione) o `http://localhost:8080/api/v1` (sviluppo)
- **Headers richiesti per tutte le API protette**:
```javascript
{
  'Authorization': `Bearer ${jwt_token}`,
  'Content-Type': 'application/json'
}
```

## 📡 API Endpoints Completi

### 1. **Health Check** (Pubblico)
```
GET /api/v1/health
```
**Response:**
```json
{
  "success": true,
  "message": "Service is running",
  "data": "OK"
}
```

### 2. **Validazione Prompt** (Principale)
```
POST /api/v1/validate-prompt
```
**Request Body:**
```json
{
  "prompt": "Notificami ogni giorno alle 9 sulle notizie",
  "email": "user@example.com",
  "channels": ["email", "discord", "slack", "whatsapp"],
  "channelConfigs": {
    "email": "user@example.com",
    "discord": "https://discord.com/api/webhooks/123/abc",
    "slack": "https://hooks.slack.com/services/T00/B00/XXX",
    "whatsapp": "+393123456789"
  },
  "timezone": "Europe/Rome"
}
```

**Success Response:**
```json
{
  "success": true,
  "message": "Prompt processed successfully",
  "data": "Risposta dettagliata di ChatGPT con la validazione del prompt..."
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Invalid prompt. Please check your input for security violations."
}
```

### 3. **Informazioni Utente**
```
GET /api/v1/user-info
```
**Response:**
```json
{
  "success": true,
  "message": "User information retrieved",
  "data": {
    "id": "auth0|123456789",
    "email": "user@example.com",
    "roles": ["ROLE_USER"]
  }
}
```

### 4. **Profilo Utente Completo**
```
GET /api/v1/user/profile
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

### 5. **Aggiorna Profilo Utente**
```
PUT /api/v1/user/profile
```
**Request Body:**
```json
{
  "displayName": "John Smith",
  "email": "john.smith@example.com"
}
```

### 6. **Aggiorna Canali di Notifica**
```
PUT /api/v1/user/notification-channels
```
**Request Body:**
```json
{
  "discord": "https://discord.com/api/webhooks/...",
  "slack": "https://hooks.slack.com/services/...",
  "whatsapp": "+393123456789"
}
```

### 7. **Statistiche Utente**
```
GET /api/v1/user/statistics
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

### 8. **Elimina Account**
```
DELETE /api/v1/user/account
```
**Response:**
```json
{
  "success": true,
  "message": "User account deleted successfully",
  "data": "OK"
}
```

## 📋 Gestione Query/Notifiche

### 9. **Lista Tutte le Query**
```
GET /api/v1/queries
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
      "dateSpecific": false,
      "toCheck": false,
      "cronParams": "0 9 * * *",
      "nextExecution": "2025-01-21T09:00:00",
      "summaryText": "Daily notification at 9 AM",
      "createdAt": "2025-01-20T10:00:00",
      "timezone": "Europe/Rome",
      "enabledChannels": "[\"email\", \"discord\"]",
      "closed": false
    }
  ]
}
```

### 10. **Query Attive**
```
GET /api/v1/queries/active
```

### 11. **Query per Tipo**
```
GET /api/v1/queries/type/{type}
```
Dove `{type}` può essere: `cron`, `specific`, `check`

### 12. **Statistiche Query**
```
GET /api/v1/queries/statistics
```
**Response:**
```json
{
  "success": true,
  "message": "Query statistics retrieved successfully",
  "data": {
    "totalQueries": 10,
    "cronQueries": 5,
    "specificQueries": 3,
    "checkQueries": 2
  }
}
```

### 13. **Chiudi Query**
```
PUT /api/v1/queries/{queryId}/close
```
**Response:**
```json
{
  "success": true,
  "message": "Query closed successfully",
  "data": "OK"
}
```

### 14. **Dettagli Query Specifica**
```
GET /api/v1/queries/{queryId}
```

## 🎨 Requisiti UI/UX

### Pagine Principali:
1. **Landing Page** - Presentazione del servizio
2. **Login/Signup** - Gestito da Auth0
3. **Dashboard** - Panoramica delle notifiche attive
4. **Crea Notifica** - Form principale per inserire prompt
5. **Le Mie Notifiche** - Lista e gestione delle notifiche
6. **Profilo** - Gestione account e canali di notifica
7. **Statistiche** - Analytics personali

### Componenti Chiave:

#### 1. **Prompt Input Component**
```jsx
// Componente principale per inserire i prompt
<PromptInput 
  onSubmit={handlePromptSubmit}
  loading={isProcessing}
  placeholder="Scrivi la tua notifica in linguaggio naturale..."
/>
```

#### 2. **Channel Selector**
```jsx
// Selezione canali di notifica
<ChannelSelector 
  availableChannels={['email', 'discord', 'slack', 'whatsapp']}
  selectedChannels={selectedChannels}
  channelConfigs={channelConfigs}
  onChange={handleChannelChange}
/>
```

#### 3. **Query Card**
```jsx
// Card per visualizzare una notifica
<QueryCard 
  query={queryData}
  onClose={handleClose}
  onEdit={handleEdit}
  showDetails={true}
/>
```

## 🔧 Gestione Errori

Tutti gli endpoint possono restituire errori nel formato:
```json
{
  "success": false,
  "error": "Messaggio di errore dettagliato"
}
```

**Codici di stato HTTP comuni:**
- `200` - Successo
- `400` - Richiesta non valida (prompt invalido, dati mancanti)
- `401` - Non autenticato (token mancante/scaduto)
- `403` - Non autorizzato
- `404` - Risorsa non trovata
- `500` - Errore interno del server

## 🌍 Supporto Timezone

Il sistema supporta timezone internazionali. Usa:
```javascript
// Per ottenere la timezone dell'utente
const userTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

// Includi sempre la timezone nelle richieste
const requestBody = {
  prompt: userPrompt,
  email: userEmail,
  timezone: userTimezone,
  // ... altri campi
};
```

## 📱 Responsive Design

L'applicazione deve essere completamente responsive e funzionare su:
- Desktop (1200px+)
- Tablet (768px - 1199px)  
- Mobile (320px - 767px)

## 🎯 Funzionalità Specifiche

### Validazione Real-time
- Validazione lunghezza prompt (max 2000 caratteri)
- Feedback visivo durante la digitazione
- Suggerimenti per migliorare i prompt

### Gestione Stati
- Loading states durante le chiamate API
- Error states con messaggi chiari
- Success states con feedback positivo

### Esempi di Prompt
Fornisci esempi di prompt validi:
- "Notificami ogni lunedì alle 9 per la riunione"
- "Ricordami il 25 dicembre di chiamare la nonna"
- "Avvisami se il prezzo di AAPL supera i 200$"
- "Notificami ogni sera alle 20 del meteo di domani"

## 🔒 Sicurezza Frontend

- Validazione input lato client (oltre a quella server)
- Sanitizzazione dati prima dell'invio
- Gestione sicura dei token JWT
- Logout automatico alla scadenza token

## 📊 Analytics e Monitoraggio

Implementa tracking per:
- Prompt creati con successo
- Errori di validazione più comuni
- Canali di notifica più utilizzati
- Tempo di utilizzo dell'app

## 🚀 Performance

- Lazy loading per le liste di notifiche
- Caching delle chiamate API quando appropriato
- Debouncing per la ricerca e filtri
- Ottimizzazione bundle size

## 🎨 Design System

Usa un design moderno e pulito con:
- Palette colori coerente
- Typography hierarchy chiara
- Spacing consistente (8px grid)
- Micro-interazioni e animazioni fluide
- Dark/Light mode support

## 📝 Note Tecniche

- Il backend gestisce automaticamente la conversione timezone
- I webhook vengono mascherati nelle risposte per sicurezza
- Le query vengono validate da ChatGPT prima del salvataggio
- Il sistema supporta notifiche ricorrenti e one-time
- Tutte le date sono gestite in UTC nel backend

Sviluppa un'interfaccia intuitiva che renda semplice per gli utenti creare notifiche complesse usando solo linguaggio naturale!