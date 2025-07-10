import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Auth0Provider } from '@auth0/auth0-react';
import { AuthGuard } from './components/AuthGuard';
import { Dashboard } from './components/Dashboard';
import { Landing } from './components/Landing';
import { Login } from './components/Login';

function App() {
  return (
    <Auth0Provider
      domain={import.meta.env.VITE_AUTH0_DOMAIN}
      clientId={import.meta.env.VITE_AUTH0_CLIENT_ID}
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: import.meta.env.VITE_AUTH0_AUDIENCE
      }}
    >
      <Router>
        <div className="min-h-screen bg-gray-50">
          <Routes>
            {/* Route principale - reindirizza alla dashboard se autenticato */}
            <Route 
              path="/" 
              element={
                <AuthGuard>
                  <Dashboard />
                </AuthGuard>
              } 
            />
            
            {/* Route di login */}
            <Route path="/login" element={<Login />} />
            
            {/* Route di landing page per utenti non autenticati */}
            <Route path="/welcome" element={<Landing />} />
            
            {/* Fallback - reindirizza alla home */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </Router>
    </Auth0Provider>
  );
}

export default App;