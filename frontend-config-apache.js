// Configurazione API per frontend con Apache
// Aggiorna il tuo file di configurazione API nel frontend

const API_CONFIG = {
  // PRODUZIONE con Apache
  PRODUCTION: {
    BASE_URL: 'https://notificamy.com',           // SENZA porta 8080
    API_BASE_URL: 'https://notificamy.com/api/v1', // Apache farà il proxy
    ENVIRONMENT: 'production'
  },
  
  // SVILUPPO locale
  DEVELOPMENT: {
    BASE_URL: 'http://localhost:8080',            // Connessione diretta per dev
    API_BASE_URL: 'http://localhost:8080/api/v1',
    ENVIRONMENT: 'development'
  }
};

// Auto-detect environment
const isProduction = window.location.hostname === 'notificamy.com' || 
                    window.location.hostname === 'www.notificamy.com';

const config = isProduction ? API_CONFIG.PRODUCTION : API_CONFIG.DEVELOPMENT;

console.log('🔧 API Configuration:', {
  BASE_URL: config.BASE_URL,
  ENV_VAR: process.env.REACT_APP_API_URL || 'not set',
  ENVIRONMENT: config.ENVIRONMENT
});

export default config;

// Esempio di utilizzo nelle chiamate API:
export const makeApiCall = async (endpoint, options = {}) => {
  const url = `${config.API_BASE_URL}${endpoint}`;
  
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  };
  
  try {
    const response = await fetch(url, defaultOptions);
    return response;
  } catch (error) {
    console.error('API call failed:', error);
    throw error;
  }
};