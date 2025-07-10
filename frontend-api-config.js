// Configurazione API per il frontend
// Questo file dovrebbe essere nel tuo progetto frontend

const API_CONFIG = {
  // PRODUZIONE: usa Nginx come proxy
  PRODUCTION: {
    BASE_URL: 'https://notificamy.com',  // SENZA porta 8080
    API_BASE_URL: 'https://notificamy.com/api/v1'
  },
  
  // SVILUPPO: connessione diretta
  DEVELOPMENT: {
    BASE_URL: 'http://localhost:8080',   // HTTP per sviluppo locale
    API_BASE_URL: 'http://localhost:8080/api/v1'
  }
};

// Rileva l'ambiente
const isProduction = window.location.hostname === 'notificamy.com';
const config = isProduction ? API_CONFIG.PRODUCTION : API_CONFIG.DEVELOPMENT;

export default config;