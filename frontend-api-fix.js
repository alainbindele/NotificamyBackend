// 🔧 FIX per il frontend - Configurazione API corretta per Apache

// ❌ PROBLEMA: Il frontend usa https://notificamy.com:8080
// ✅ SOLUZIONE: Il frontend deve usare https://notificamy.com (Apache proxy)

const API_CONFIG = {
  // PRODUZIONE: usa Apache come proxy (SENZA porta 8080)
  PRODUCTION: {
    BASE_URL: 'https://notificamy.com',           // ✅ Apache gestisce HTTPS
    API_BASE_URL: 'https://notificamy.com/api/v1' // ✅ Apache fa proxy a :8080
  },
  
  // SVILUPPO: connessione diretta
  DEVELOPMENT: {
    BASE_URL: 'http://localhost:8080',            // ✅ HTTP diretto per dev
    API_BASE_URL: 'http://localhost:8080/api/v1'
  }
};

// Auto-detect environment
const isProduction = window.location.hostname === 'notificamy.com' || 
                    window.location.hostname === 'www.notificamy.com';

const config = isProduction ? API_CONFIG.PRODUCTION : API_CONFIG.DEVELOPMENT;

console.log('🔧 API Configuration:', {
  BASE_URL: config.BASE_URL,
  HOSTNAME: window.location.hostname,
  ENVIRONMENT: isProduction ? 'production' : 'development'
});

export default config;

// ✅ Esempio di chiamata API corretta:
export const makeApiCall = async (endpoint, token) => {
  const url = `${config.API_BASE_URL}${endpoint}`;
  
  console.log('Making API request to:', url); // Per debug
  
  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  return response;
};

// 📝 ISTRUZIONI PER IL FRONTEND:
// 1. Sostituisci tutte le occorrenze di "https://notificamy.com:8080" 
//    con "https://notificamy.com"
// 2. Rimuovi la porta 8080 dalle chiamate API in produzione
// 3. Apache farà automaticamente il proxy verso localhost:8080