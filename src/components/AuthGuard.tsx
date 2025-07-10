import React from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { Navigate } from 'react-router-dom';
import { LoadingSpinner } from './LoadingSpinner';

interface AuthGuardProps {
  children: React.ReactNode;
}

export const AuthGuard: React.FC<AuthGuardProps> = ({ children }) => {
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();

  // Mostra loading durante il controllo dell'autenticazione
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  // Se non autenticato, reindirizza al login
  if (!isAuthenticated) {
    // Avvia automaticamente il processo di login
    loginWithRedirect({
      appState: {
        returnTo: window.location.pathname
      }
    });
    
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <LoadingSpinner />
          <p className="mt-4 text-gray-600">Redirecting to login...</p>
        </div>
      </div>
    );
  }

  // Se autenticato, mostra il contenuto
  return <>{children}</>;
};