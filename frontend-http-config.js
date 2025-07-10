// Configurazione frontend per HTTP (senza HTTPS obbligatorio)

const API_CONFIG = {
  // PRODUZIONE: usa HTTP tramite Apache proxy
  PRODUCTION: {
    BASE_URL: 'http://notificamy.com',           // HTTP permesso
    API_BASE_URL: 'http://notificamy.com/api/v1', // Apache fa proxy a :8080
    ENVIRONMENT: 'production'
  },
  
  // SVILUPPO: connessione diretta
  DEVELOPMENT: {
    BASE_URL: 'http://localhost:8080',
    API_BASE_URL: 'http://localhost:8080/api/v1',
    ENVIRONMENT: 'development'
  }
};

// Auto-detect environment
const isProduction = window.location.hostname === 'notificamy.com' || 
                    window.location.hostname === 'www.notificamy.com';

const config = isProduction ? API_CONFIG.PRODUCTION : API_CONFIG.DEVELOPMENT;

console.log('🔧 API Configuration (HTTP allowed):', {
  BASE_URL: config.BASE_URL,
  HOSTNAME: window.location.hostname,
  PROTOCOL: window.location.protocol,
  ENVIRONMENT: config.ENVIRONMENT
});

export default config;

// Esempio di chiamata API
export const makeApiCall = async (endpoint, token, options = {}) => {
  const url = `${config.API_BASE_URL}${endpoint}`;
  
  console.log('Making API request to:', url);
  
  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  });
  
  return response;
};

// 📝 ISTRUZIONI:
// 1. Il frontend ora può usare HTTP in produzione
// 2. Rimuovi tutte le forzature HTTPS dal codice frontend
// 3. Usa http://notificamy.com invece di https://notificamy.com:8080