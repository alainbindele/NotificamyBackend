# Configurazione Logto per Notificamy Backend

Questo documento spiega come configurare l'autenticazione Logto nel backend di Notificamy.

## Migrazione da Clerk a Logto

Il backend è stato migrato da Clerk a Logto per l'autenticazione. Le modifiche principali includono:

### File Modificati

1. **application.yml** - Configurazione aggiornata per Logto
2. **pom.xml** - Aggiunta dipendenza Nimbus JOSE+JWT per validazione JWKS
3. **JwtAuthenticationFilter.java** - Refactoring completo per validare JWT Logto
4. **SecurityConfig.java** - Rimossi commenti specifici per Clerk
5. **.env** - Aggiunte variabili d'ambiente per Logto

## Configurazione

### Passo 1: Ottenere le credenziali Logto

1. Accedi alla tua console Logto (https://cloud.logto.io o self-hosted)
2. Crea una nuova applicazione (tipo: "Single Page Application" o "Traditional Web")
3. Prendi nota di:
   - **Endpoint**: L'URL dell'istanza Logto (es. `https://your-tenant.logto.app`)
   - **App ID**: L'identificatore dell'applicazione

**Nota**: Non è necessario un App Secret. Il backend valida i JWT usando le chiavi pubbliche JWKS di Logto.

### Passo 2: Configurare le variabili d'ambiente

Aggiorna il file `.env` con le tue credenziali Logto:

```env
# Logto Configuration
LOGTO_ENDPOINT=https://your-tenant.logto.app
LOGTO_APP_ID=your_app_id_here
```

Oppure esporta le variabili d'ambiente direttamente:

```bash
export LOGTO_ENDPOINT=https://your-tenant.logto.app
export LOGTO_APP_ID=your_app_id_here
```

**Note**:
- Il campo `LOGTO_ISSUER` è opzionale. Se non specificato, il sistema userà automaticamente `${LOGTO_ENDPOINT}/oidc`
- Non è necessario `LOGTO_APP_SECRET` - la validazione JWT usa JWKS (chiavi pubbliche)
- **L'applicazione può avviarsi anche senza configurazione Logto**, ma mostrerà un warning e tutti gli endpoint protetti restituiranno 401

### Passo 3: Configurare Logto sul Frontend

Sul frontend, configura il client Logto per inviare il token JWT nell'header `Authorization`:

```typescript
// Esempio configurazione frontend
const config = {
  endpoint: 'https://your-tenant.logto.app',
  appId: 'your_app_id_here',
  resources: ['https://api.notificamy.com'], // Il tuo backend API
  scopes: ['email', 'profile'] // Richiedi gli scope necessari
};
```

Assicurati che il frontend includa il token JWT in ogni richiesta API:

```typescript
headers: {
  'Authorization': `Bearer ${accessToken}`
}
```

## Come Funziona

### Validazione JWT

Il backend valida i token JWT di Logto seguendo questi step:

1. **Estrae il token** dall'header `Authorization: Bearer <token>`
2. **Scarica le chiavi pubbliche** dall'endpoint JWKS di Logto (`${LOGTO_ENDPOINT}/oidc/jwks`)
3. **Verifica la firma** del token usando le chiavi pubbliche
4. **Valida i claim**:
   - `iss` (issuer): deve corrispondere a `${LOGTO_ENDPOINT}/oidc`
   - `aud` (audience): deve corrispondere al tuo `LOGTO_APP_ID`
   - `exp` (expiration): il token non deve essere scaduto
5. **Estrae le informazioni utente**:
   - `sub`: ID utente
   - `email`: Email dell'utente (se presente negli scope)

### Endpoints Pubblici

I seguenti endpoint sono accessibili senza autenticazione:
- `/api/v1/health`
- `/actuator/health`
- `/actuator/**`
- `/error`

Tutti gli altri endpoint sotto `/api/v1/**` richiedono un token JWT valido.

## Scope Logto Richiesti

Per funzionare correttamente, l'applicazione Logto deve richiedere almeno i seguenti scope:

- `openid`: Obbligatorio per OIDC
- `profile`: Informazioni base dell'utente
- `email`: Email dell'utente

Puoi configurare gli scope nella console Logto o nel client frontend.

## Troubleshooting

### Token non valido

Se ricevi l'errore "Invalid or expired token":

1. Verifica che `LOGTO_ENDPOINT` sia corretto
2. Verifica che `LOGTO_APP_ID` corrisponda all'audience nel token
3. Controlla che il token non sia scaduto
4. Assicurati che il frontend richieda gli scope corretti

### Email non trovata

Se l'email non è presente nel token:
- Verifica che lo scope `email` sia richiesto nel frontend
- Controlla la configurazione dell'applicazione in Logto
- L'applicazione userà un placeholder `no-email@logto.user` se l'email non è disponibile

### Errori JWKS

Se ci sono problemi con la validazione JWKS:
- Verifica che l'endpoint Logto sia raggiungibile
- Controlla che `${LOGTO_ENDPOINT}/oidc/jwks` sia accessibile
- Verifica la connessione internet del server

## Test

Per testare l'autenticazione:

```bash
# 1. Ottieni un token dal frontend Logto
# 2. Testa un endpoint protetto
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/user/profile

# 3. Verifica che gli endpoint pubblici funzionino senza token
curl http://localhost:8080/api/v1/health
```

## Note Aggiuntive

- Il sistema usa **Nimbus JOSE+JWT** per la validazione JWT con supporto JWKS
- La cache JWKS è gestita automaticamente dalla libreria Nimbus
- I token sono validati ad ogni richiesta (stateless)
- Non è necessario chiamare API Logto per ogni validazione grazie a JWKS
