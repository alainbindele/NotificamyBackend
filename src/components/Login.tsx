import React, { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { Navigate } from 'react-router-dom';
import { LoadingSpinner } from './LoadingSpinner';

export const Login: React.FC = () => {
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      loginWithRedirect({
        appState: {
          returnTo: '/'
        }
      });
    }
  }, [isLoading, isAuthenticated, loginWithRedirect]);

  // Se già autenticato, reindirizza alla dashboard
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  // Mostra loading durante il processo di login
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <LoadingSpinner />
        <p className="mt-4 text-gray-600">Redirecting to login...</p>
      </div>
    </div>
  );
};