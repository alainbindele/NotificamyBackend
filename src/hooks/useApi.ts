import { useAuth0 } from '@auth0/auth0-react';
import { useCallback } from 'react';

interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data?: T;
  error?: string;
}

export const useApi = () => {
  const { getAccessTokenSilently } = useAuth0();

  const apiCall = useCallback(async <T = any>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> => {
    try {
      const token = await getAccessTokenSilently({
        audience: import.meta.env.VITE_AUTH0_AUDIENCE
      });

      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}${endpoint}`, {
        ...options,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
          ...options.headers,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('API call failed:', error);
      return {
        success: false,
        message: 'API call failed',
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }, [getAccessTokenSilently]);

  return { apiCall };
};