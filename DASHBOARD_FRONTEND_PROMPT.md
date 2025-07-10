# 🎯 Prompt per Sviluppo Dashboard Frontend React TypeScript

## 📋 **Obiettivo**
Sviluppa una dashboard completa in **React + TypeScript** per gestire utenti e notifiche, comunicando con il backend NotifyMe tramite API REST autenticate con Auth0 JWT.

## 🔐 **Autenticazione**
- **Sistema:** Auth0 JWT Bearer Token
- **Header richiesto:** `Authorization: Bearer <jwt-token>`
- **Gestione:** Usa `@auth0/auth0-react` per ottenere token automaticamente
- **Configurazione Auth0:**
  ```typescript
  const auth0Config = {
    domain: "your-domain.auth0.com",
    clientId: "your-client-id",
    authorizationParams: {
      redirect_uri: window.location.origin,
      audience: "https://notificamy.com/api"
    }
  }
  ```

## 🏗️ **Struttura Dashboard**
Crea una dashboard con le seguenti sezioni:
1. **Profilo Utente** - Gestione account e canali notifica
2. **Le Mie Notifiche** - CRUD completo delle query
3. **Statistiche** - Analytics e metriche utente

---

## 👤 **SEZIONE 1: GESTIONE UTENTE**

### 🔍 **GET /api/v1/user/profile** - Profilo Utente
**Scopo:** Recupera il profilo completo dell'utente

**Headers:**
```json
{
  "Authorization": "Bearer <jwt-token>",
  "Content-Type": "application/json"
}
```

**Risposta Successo (200):**
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

**Risposta Errore (400/401/500):**
```json
{
  "success": false,
  "error": "User not found"
}
```

### ✏️ **PUT /api/v1/user/profile** - Aggiorna Profilo
**Scopo:** Modifica nome visualizzato e/o email

**Input JSON:**
```json
{
  "displayName": "John Smith",
  "email": "john.smith@example.com"
}
```

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "User profile updated successfully",
  "data": {
    "id": 1,
    "email": "john.smith@example.com",
    "displayName": "John Smith",
    "createdAt": "2025-01-20T10:00:00",
    "discordWebhook": "https://dis***...xyz",
    "slackWebhook": "https://hoo***...abc",
    "phone": "+39***...789"
  }
}
```

### 🔔 **PUT /api/v1/user/notification-channels** - Canali Notifica
**Scopo:** Configura webhook Discord, Slack e numero WhatsApp

**Input JSON:**
```json
{
  "discord": "https://discord.com/api/webhooks/123456789/abcdefghijklmnop",
  "slack": "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX",
  "whatsapp": "+393123456789"
}
```

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "Notification channels updated successfully",
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

### 📊 **GET /api/v1/user/statistics** - Statistiche Utente
**Scopo:** Metriche e analytics dell'account

**Risposta Successo (200):**
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

### 🗑️ **DELETE /api/v1/user/account** - Elimina Account
**Scopo:** Cancellazione completa account e dati

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "User account deleted successfully",
  "data": "OK"
}
```

---

## 🔔 **SEZIONE 2: GESTIONE NOTIFICHE**

### ➕ **POST /api/v1/validate-prompt** - Crea Notifica
**Scopo:** Crea una nuova notifica tramite prompt naturale

**Input JSON:**
```json
{
  "prompt": "Notificami ogni giorno alle 9 sulle notizie di tecnologia",
  "email": "user@example.com",
  "channels": ["email", "discord"],
  "channelConfigs": {
    "email": "user@example.com",
    "discord": "https://discord.com/api/webhooks/123456789/abcdefghijklmnop"
  },
  "timezone": "Europe/Rome"
}
```

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "Prompt processed successfully",
  "data": "Notifica creata con successo: ogni giorno alle 9:00 riceverai aggiornamenti sulle notizie di tecnologia tramite email e Discord."
}
```

**Risposta Errore (400):**
```json
{
  "success": false,
  "error": "Il prompt non è valido secondo le policy di sistema"
}
```

### 📋 **GET /api/v1/queries** - Tutte le Notifiche
**Scopo:** Recupera tutte le notifiche dell'utente

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "Queries retrieved successfully",
  "data": [
    {
      "id": 1,
      "prompt": "Notificami ogni giorno alle 9 sulle notizie",
      "isValid": true,
      "closed": false,
      "cron": true,
      "dateSpecific": false,
      "toCheck": false,
      "cronParams": "0 9 * * *",
      "nextExecution": "2025-01-21T09:00:00",
      "summaryText": "Notifica giornaliera alle 9:00 per notizie",
      "createdAt": "2025-01-20T10:00:00",
      "timezone": "Europe/Rome",
      "enabledChannels": "[\"email\", \"discord\"]"
    },
    {
      "id": 2,
      "prompt": "Ricordami di chiamare il dottore il 25 gennaio",
      "isValid": true,
      "closed": false,
      "cron": false,
      "dateSpecific": true,
      "toCheck": false,
      "nextExecution": "2025-01-25T10:00:00",
      "summaryText": "Promemoria chiamata dottore",
      "createdAt": "2025-01-20T11:00:00",
      "timezone": "Europe/Rome"
    }
  ]
}
```

### ✅ **GET /api/v1/queries/active** - Notifiche Attive
**Scopo:** Solo notifiche valide e non chiuse

**Risposta:** Stesso formato di `/queries` ma filtrato per `isValid: true` e `closed: false`

### 🏷️ **GET /api/v1/queries/type/{type}** - Per Categoria
**Scopo:** Filtra per tipo di notifica
- **Parametri:** `type` = `cron` | `specific` | `check`

**Esempio:** `GET /api/v1/queries/type/cron`

### 🔍 **GET /api/v1/queries/{queryId}** - Singola Notifica
**Scopo:** Dettagli di una notifica specifica

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "Query retrieved successfully",
  "data": {
    "id": 1,
    "prompt": "Notificami ogni giorno alle 9 sulle notizie",
    "isValid": true,
    "closed": false,
    "cron": true,
    "dateSpecific": false,
    "toCheck": false,
    "cronParams": "0 9 * * *",
    "nextExecution": "2025-01-21T09:00:00",
    "validFrom": null,
    "validTo": null,
    "summaryText": "Notifica giornaliera alle 9:00 per notizie",
    "language": "it",
    "category": "notification_generation",
    "confidenceScore": 0.95,
    "createdAt": "2025-01-20T10:00:00",
    "timezone": "Europe/Rome",
    "enabledChannels": "[\"email\", \"discord\"]"
  }
}
```

### 📊 **GET /api/v1/queries/statistics** - Statistiche Notifiche
**Scopo:** Metriche sulle notifiche dell'utente

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "Query statistics retrieved successfully",
  "data": {
    "totalQueries": 10,
    "cronQueries": 6,
    "specificQueries": 3,
    "checkQueries": 1
  }
}
```

### ❌ **PUT /api/v1/queries/{queryId}/close** - Chiudi Notifica
**Scopo:** Disattiva definitivamente una notifica

**Risposta Successo (200):**
```json
{
  "success": true,
  "message": "Query closed successfully",
  "data": "OK"
}
```

**Risposta Errore (400/404):**
```json
{
  "success": false,
  "error": "Failed to close query. Query not found or access denied."
}
```

---

## 🎨 **COMPONENTI REACT DA IMPLEMENTARE**

### 1. **UserProfileCard**
```typescript
interface UserProfile {
  id: number;
  email: string;
  displayName: string;
  createdAt: string;
  discordWebhook?: string;
  slackWebhook?: string;
  phone?: string;
}

const UserProfileCard: React.FC = () => {
  // Gestisce GET /api/v1/user/profile
  // Form per PUT /api/v1/user/profile
  // Form per PUT /api/v1/user/notification-channels
}
```

### 2. **NotificationsList**
```typescript
interface Query {
  id: number;
  prompt: string;
  isValid: boolean;
  closed: boolean;
  cron: boolean;
  dateSpecific: boolean;
  toCheck: boolean;
  cronParams?: string;
  nextExecution?: string;
  summaryText?: string;
  createdAt: string;
  timezone?: string;
  enabledChannels?: string;
}

const NotificationsList: React.FC = () => {
  // Gestisce GET /api/v1/queries
  // Filtri per tipo (cron/specific/check)
  // Azioni per ogni notifica (visualizza, chiudi)
}
```

### 3. **CreateNotificationForm**
```typescript
interface CreateNotificationRequest {
  prompt: string;
  email: string;
  channels: string[];
  channelConfigs: Record<string, string>;
  timezone: string;
}

const CreateNotificationForm: React.FC = () => {
  // Form per POST /api/v1/validate-prompt
  // Validazione input lato client
  // Gestione errori e successo
}
```

### 4. **DashboardStats**
```typescript
interface UserStats {
  totalQueries: number;
  activeQueries: number;
  cronQueries: number;
  specificQueries: number;
  checkQueries: number;
  configuredChannels: number;
  daysSinceRegistration: number;
}

const DashboardStats: React.FC = () => {
  // Gestisce GET /api/v1/user/statistics
  // Visualizzazione con card e grafici
}
```

---

## 🔧 **IMPLEMENTAZIONE TECNICA**

### **Hook Personalizzato per API**
```typescript
const useNotifyMeAPI = () => {
  const { getAccessTokenSilently } = useAuth0();
  
  const apiCall = async (endpoint: string, options?: RequestInit) => {
    const token = await getAccessTokenSilently({
      audience: 'https://notificamy.com/api'
    });
    
    const response = await fetch(`/api/v1${endpoint}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        ...options?.headers
      }
    });
    
    return response.json();
  };
  
  return { apiCall };
};
```

### **Gestione Stati Notifiche**
```typescript
const getNotificationStatus = (query: Query) => {
  if (!query.isValid) return { status: 'invalid', color: 'red', icon: '❌' };
  if (query.closed) return { status: 'closed', color: 'gray', icon: '🔒' };
  
  const now = new Date();
  const nextExec = query.nextExecution ? new Date(query.nextExecution) : null;
  
  if (nextExec && nextExec < now) {
    return { status: 'expired', color: 'orange', icon: '⏰' };
  }
  
  return { status: 'active', color: 'green', icon: '✅' };
};
```

### **Tipi di Notifica**
```typescript
const getNotificationType = (query: Query) => {
  if (query.cron && !query.dateSpecific && !query.toCheck) {
    return { type: 'recurring', label: 'Ricorrente', icon: '🔄' };
  }
  if (!query.cron && query.dateSpecific && !query.toCheck) {
    return { type: 'scheduled', label: 'Programmata', icon: '📅' };
  }
  if (query.toCheck) {
    return { type: 'conditional', label: 'Condizionale', icon: '🔍' };
  }
  return { type: 'unknown', label: 'Sconosciuto', icon: '❓' };
};
```

---

## 🎯 **FUNZIONALITÀ DASHBOARD**

### **Sezione Profilo Utente:**
- ✅ Visualizzazione dati account
- ✅ Modifica nome e email
- ✅ Configurazione canali notifica (Discord, Slack, WhatsApp)
- ✅ Statistiche account (giorni registrazione, canali configurati)
- ✅ Eliminazione account

### **Sezione Notifiche:**
- ✅ Lista completa notifiche con filtri
- ✅ Creazione nuove notifiche tramite prompt
- ✅ Visualizzazione dettagli singola notifica
- ✅ Chiusura/disattivazione notifiche
- ✅ Badge di stato (attiva, chiusa, scaduta, non valida)
- ✅ Icone per tipo (ricorrente, programmata, condizionale)

### **Sezione Analytics:**
- ✅ Contatori per tipo di notifica
- ✅ Statistiche utilizzo
- ✅ Grafici e metriche

---

## 🚨 **GESTIONE ERRORI**

### **Errori Comuni:**
- **401 Unauthorized:** Token scaduto/invalido → Redirect a login
- **400 Bad Request:** Input non valido → Mostra messaggio specifico
- **404 Not Found:** Risorsa non trovata → Messaggio user-friendly
- **500 Server Error:** Errore server → "Riprova più tardi"

### **Pattern di Gestione:**
```typescript
const handleAPIError = (error: any) => {
  if (error.status === 401) {
    // Redirect to login
    window.location.href = '/login';
  } else if (error.status === 400) {
    // Show validation error
    setError(error.error || 'Dati non validi');
  } else {
    // Generic error
    setError('Si è verificato un errore. Riprova più tardi.');
  }
};
```

---

## 🎨 **UI/UX REQUIREMENTS**

- **Design:** Moderno, pulito, responsive
- **Tema:** Supporto dark/light mode
- **Accessibilità:** ARIA labels, keyboard navigation
- **Performance:** Loading states, lazy loading
- **Feedback:** Toast notifications per azioni
- **Validazione:** Real-time validation sui form

Implementa una dashboard completa e user-friendly che sfrutti tutti gli endpoint disponibili per offrire un'esperienza di gestione notifiche ottimale!