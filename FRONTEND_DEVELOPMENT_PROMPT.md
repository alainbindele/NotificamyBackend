# Prompt per Sviluppo Frontend NotifyMe

Sei un esperto sviluppatore frontend che deve creare un'applicazione React moderna per il sistema NotifyMe. Il backend è già sviluppato e funzionante con autenticazione Auth0 e API REST complete.

## 🎯 Obiettivo del Progetto

NotifyMe è un sistema di notifiche intelligenti che permette agli utenti di creare notifiche personalizzate usando linguaggio naturale. Gli utenti possono dire cose come:
- "Notificami ogni giorno alle 9 sulle notizie"
- "Ricordami il 21 gennaio di chiamare mamma"
- "Avvisami se il prezzo di Bitcoin scende sotto i 50.000$"

Il sistema usa ChatGPT per interpretare e validare i prompt degli utenti.

## 🔄 **OPERAZIONI CRUD PER LE NOTIFICHE**

### **CREATE - Creazione Nuove Notifiche**

**Endpoint:** `POST /api/v1/validate-prompt`

**Quando usare:** Quando l'utente vuole creare una nuova notifica tramite prompt naturale.

**Request Body:**
```json
{
  "prompt": "Notificami ogni giorno alle 9 sulle notizie di tecnologia",
  "email": "user@example.com",
  "channels": ["email", "discord"],
  "channelConfigs": {
    "email": "user@example.com",
    "discord": "https://discord.com/api/webhooks/123/abc"
  },
  "timezone": "Europe/Rome"
}
```

**Response Success (200):**
```json
{
  "success": true,
  "message": "Prompt processed successfully",
  "data": "Notifica creata con successo. Riceverai aggiornamenti quotidiani alle 9:00 sui canali selezionati."
}
```

**Response Error (400):**
```json
{
  "success": false,
  "error": "Il prompt contiene linguaggio offensivo o non è valido"
}
```

**Implementazione Frontend:**
```javascript
const createNotification = async (promptData) => {
  try {
    const token = await getAccessTokenSilently();
    const response = await fetch('/api/v1/validate-prompt', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        ...promptData,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
      })
    });
    
    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.error);
    }
    
    return result;
  } catch (error) {
    console.error('Error creating notification:', error);
    throw error;
  }
};
```

---

### **READ - Lettura Notifiche Esistenti**

#### **1. Tutte le Notifiche dell'Utente**

**Endpoint:** `GET /api/v1/queries`

**Quando usare:** Per mostrare la lista completa delle notifiche dell'utente nella dashboard.

**Response (200):**
```json
{
  "success": true,
  "message": "Queries retrieved successfully",
  "data": [
    {
      "id": 1,
      "prompt": "Notificami ogni giorno alle 9 sulle notizie",
      "isValid": true,
      "cron": true,
      "dateSpecific": false,
      "toCheck": false,
      "cronParams": "0 9 * * *",
      "nextExecution": "2025-01-21T09:00:00",
      "summaryText": "Notifica quotidiana alle 9:00 per notizie",
      "createdAt": "2025-01-20T10:00:00",
      "closed": false,
      "timezone": "Europe/Rome",
      "enabledChannels": "[\"email\", \"discord\"]"
    }
  ]
}
```

#### **2. Solo Notifiche Attive**

**Endpoint:** `GET /api/v1/queries/active`

**Quando usare:** Per mostrare solo le notifiche attualmente in funzione.

#### **3. Notifiche per Tipo**

**Endpoint:** `GET /api/v1/queries/type/{type}`

**Parametri:** `type` può essere `cron`, `specific`, o `check`

**Quando usare:** Per filtrare le notifiche per categoria (ricorrenti, specifiche, condizionali).

#### **4. Singola Notifica**

**Endpoint:** `GET /api/v1/queries/{queryId}`

**Quando usare:** Per visualizzare i dettagli completi di una notifica specifica.

**Implementazione Frontend:**
```javascript
const fetchNotifications = async (filter = 'all') => {
  try {
    const token = await getAccessTokenSilently();
    let endpoint = '/api/v1/queries';
    
    switch(filter) {
      case 'active':
        endpoint = '/api/v1/queries/active';
        break;
      case 'cron':
      case 'specific':
      case 'check':
        endpoint = `/api/v1/queries/type/${filter}`;
        break;
    }
    
    const response = await fetch(endpoint, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const result = await response.json();
    return result.data || [];
  } catch (error) {
    console.error('Error fetching notifications:', error);
    return [];
  }
};
```

---

### **UPDATE - Modifica Notifiche**

**IMPORTANTE:** Il backend NotifyMe non supporta la modifica diretta delle notifiche esistenti per motivi di sicurezza e integrità dei dati. Invece, utilizza il pattern "Close & Recreate".

#### **Chiusura Notifica (Equivalente a DELETE/UPDATE)**

**Endpoint:** `PUT /api/v1/queries/{queryId}/close`

**Quando usare:** 
- Per "eliminare" una notifica (la chiude definitivamente)
- Per "modificare" una notifica (chiudi la vecchia e creane una nuova)

**Response (200):**
```json
{
  "success": true,
  "message": "Query closed successfully",
  "data": "OK"
}
```

**Response Error (400):**
```json
{
  "success": false,
  "error": "Failed to close query. Query not found or access denied."
}
```

**Implementazione Frontend:**
```javascript
const closeNotification = async (queryId) => {
  try {
    const token = await getAccessTokenSilently();
    const response = await fetch(`/api/v1/queries/${queryId}/close`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.error);
    }
    
    return result;
  } catch (error) {
    console.error('Error closing notification:', error);
    throw error;
  }
};

// Funzione per "modificare" una notifica (pattern Close & Recreate)
const updateNotification = async (queryId, newPromptData) => {
  try {
    // 1. Chiudi la notifica esistente
    await closeNotification(queryId);
    
    // 2. Crea una nuova notifica con i dati aggiornati
    const newNotification = await createNotification(newPromptData);
    
    return newNotification;
  } catch (error) {
    console.error('Error updating notification:', error);
    throw error;
  }
};
```

---

### **DELETE - Eliminazione Notifiche**

**Metodo:** Utilizza lo stesso endpoint di UPDATE (chiusura)

**Endpoint:** `PUT /api/v1/queries/{queryId}/close`

**Quando usare:** Quando l'utente vuole eliminare definitivamente una notifica.

**Implementazione Frontend:**
```javascript
const deleteNotification = async (queryId) => {
  // Stessa implementazione di closeNotification
  return await closeNotification(queryId);
};
```

---

## 🎛️ **GESTIONE STATI DELLE NOTIFICHE**

### **Stati Possibili:**

1. **Attiva (`isValid: true, closed: false`)** - Notifica funzionante
2. **Chiusa (`closed: true`)** - Notifica eliminata dall'utente
3. **Non Valida (`isValid: false`)** - Notifica rifiutata da ChatGPT
4. **Scaduta** - Notifica con `validTo` nel passato

### **Componente React per Gestione Stati:**

```javascript
const NotificationCard = ({ notification, onClose, onEdit }) => {
  const getStatusBadge = () => {
    if (notification.closed) {
      return <Badge variant="secondary">Chiusa</Badge>;
    }
    if (!notification.isValid) {
      return <Badge variant="destructive">Non Valida</Badge>;
    }
    if (notification.validTo && new Date(notification.validTo) < new Date()) {
      return <Badge variant="outline">Scaduta</Badge>;
    }
    return <Badge variant="success">Attiva</Badge>;
  };

  const getTypeIcon = () => {
    if (notification.cron) return <Clock className="w-4 h-4" />;
    if (notification.dateSpecific) return <Calendar className="w-4 h-4" />;
    if (notification.toCheck) return <Search className="w-4 h-4" />;
    return <Bell className="w-4 h-4" />;
  };

  return (
    <Card className="p-4">
      <div className="flex justify-between items-start">
        <div className="flex items-center gap-2">
          {getTypeIcon()}
          <h3 className="font-medium">{notification.summaryText}</h3>
        </div>
        {getStatusBadge()}
      </div>
      
      <p className="text-sm text-gray-600 mt-2">{notification.prompt}</p>
      
      <div className="flex justify-between items-center mt-4">
        <span className="text-xs text-gray-500">
          Prossima esecuzione: {notification.nextExecution ? 
            new Date(notification.nextExecution).toLocaleString() : 'N/A'}
        </span>
        
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => onEdit(notification)}>
            Modifica
          </Button>
          <Button variant="destructive" size="sm" onClick={() => onClose(notification.id)}>
            Elimina
          </Button>
        </div>
      </div>
    </Card>
  );
};
```

---

## 📊 **STATISTICHE E ANALYTICS**

**Endpoint:** `GET /api/v1/queries/statistics`

**Response:**
```json
{
  "success": true,
  "data": {
    "totalQueries": 15,
    "cronQueries": 8,
    "specificQueries": 4,
    "checkQueries": 3
  }
}
```

**Implementazione Dashboard:**
```javascript
const StatisticsWidget = () => {
  const [stats, setStats] = useState(null);
  
  useEffect(() => {
    const fetchStats = async () => {
      try {
        const token = await getAccessTokenSilently();
        const response = await fetch('/api/v1/queries/statistics', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        const result = await response.json();
        setStats(result.data);
      } catch (error) {
        console.error('Error fetching statistics:', error);
      }
    };
    
    fetchStats();
  }, []);
  
  if (!stats) return <div>Caricamento...</div>;
  
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      <StatCard title="Totale" value={stats.totalQueries} icon={<Bell />} />
      <StatCard title="Ricorrenti" value={stats.cronQueries} icon={<Clock />} />
      <StatCard title="Specifiche" value={stats.specificQueries} icon={<Calendar />} />
      <StatCard title="Condizionali" value={stats.checkQueries} icon={<Search />} />
    </div>
  );
};
```

---

## 🔄 **PATTERN DI UTILIZZO RACCOMANDATI**

### **1. Flusso Creazione Notifica:**
```
TUser Input → Validation → Channel Selection → Create → Success/Error Feedback
```

### **2. Flusso Modifica Notifica:**
```
Select Notification → Load Data → Edit Form → Close Old → Create New → Refresh List
```

### **3. Flusso Eliminazione:**
```
Select Notification → Confirm Dialog → Close API Call → Remove from UI → Success Message
```

### **4. Gestione Errori:**
```javascript
const handleApiError = (error, operation) => {
  const errorMessages = {
    'Invalid prompt': 'Il prompt contiene contenuto non valido. Riprova con una formulazione diversa.',
    'Authorization token required': 'Sessione scaduta. Effettua nuovamente il login.',
    'User not found': 'Errore di autenticazione. Ricarica la pagina.',
    'Failed to close query': 'Impossibile eliminare la notifica. Riprova più tardi.'
  };
  
  const message = errorMessages[error.message] || `Errore durante ${operation}. Riprova più tardi.`;
  
  toast.error(message);
  console.error(`${operation} error:`, error);
};
```
