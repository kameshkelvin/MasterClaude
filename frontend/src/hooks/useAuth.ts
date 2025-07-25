import { useState, useEffect, useCallback } from 'react';
import { authApi, handleApiError } from '@/lib/api';
import { storage } from '@/lib/utils';
import { AuthUser, LoginForm, RegisterForm, UseAuthReturn } from '@/types';

export const useAuth = (): UseAuthReturn => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);

  // Initialize auth state from storage
  useEffect(() => {
    const initAuth = () => {
      try {
        const storedUser = storage.get<AuthUser | null>('user', null);
        if (storedUser && storedUser.accessToken) {
          setUser(storedUser);
        }
      } catch (error) {
        console.error('Error initializing auth:', error);
        storage.remove('user');
      } finally {
        setIsInitializing(false);
      }
    };

    initAuth();
  }, []);

  // Login function
  const login = useCallback(async (credentials: LoginForm) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(credentials);
      const userData = response.data.data;

      if (userData) {
        setUser(userData);
        storage.set('user', userData);

        // Store remember me preference
        if (credentials.rememberMe) {
          storage.set('rememberMe', true);
        } else {
          storage.remove('rememberMe');
        }
      }
    } catch (error: any) {
      const errorMessage = handleApiError(error);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Register function
  const register = useCallback(async (data: RegisterForm) => {
    setIsLoading(true);
    try {
      const response = await authApi.register(data);
      // Don't auto-login after registration, let user login manually
      return response.data;
    } catch (error: any) {
      const errorMessage = handleApiError(error);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Logout function
  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      // Call API to invalidate token on server
      if (user?.accessToken) {
        await authApi.logout();
      }
    } catch (error) {
      // Continue with logout even if API call fails
      console.error('Logout API call failed:', error);
    } finally {
      // Clear local state and storage
      setUser(null);
      storage.remove('user');
      storage.remove('rememberMe');
      setIsLoading(false);
    }
  }, [user]);

  // Refresh token function
  const refreshToken = useCallback(async () => {
    if (!user?.refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const response = await authApi.refreshToken(user.refreshToken);
      const userData = response.data.data;

      if (userData) {
        setUser(userData);
        storage.set('user', userData);
        return userData;
      } else {
        throw new Error('Invalid response from refresh token');
      }
    } catch (error: any) {
      // If refresh fails, logout user
      setUser(null);
      storage.remove('user');
      throw new Error(handleApiError(error));
    }
  }, [user]);

  // Update user profile
  const updateProfile = useCallback((updatedData: Partial<AuthUser>) => {
    if (user) {
      const updatedUser = { ...user, ...updatedData };
      setUser(updatedUser);
      storage.set('user', updatedUser);
    }
  }, [user]);

  // Check if user is authenticated
  const isAuthenticated = Boolean(user?.accessToken);

  // Auto-refresh token when it's about to expire
  useEffect(() => {
    if (!user?.accessToken) return;

    const checkTokenExpiry = () => {
      try {
        // Decode JWT to check expiry (simple base64 decode)
        const payload = JSON.parse(atob(user.accessToken.split('.')[1]));
        const currentTime = Date.now() / 1000;
        const timeUntilExpiry = payload.exp - currentTime;

        // Refresh token if it expires in less than 5 minutes
        if (timeUntilExpiry < 300 && timeUntilExpiry > 0) {
          refreshToken().catch((error) => {
            console.error('Auto token refresh failed:', error);
          });
        }
      } catch (error) {
        console.error('Error checking token expiry:', error);
      }
    };

    // Check immediately and then every minute
    checkTokenExpiry();
    const interval = setInterval(checkTokenExpiry, 60000);

    return () => clearInterval(interval);
  }, [user, refreshToken]);

  // Handle storage changes (for cross-tab synchronization)
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'user') {
        if (e.newValue) {
          try {
            const newUser = JSON.parse(e.newValue);
            setUser(newUser);
          } catch (error) {
            console.error('Error parsing user from storage:', error);
          }
        } else {
          setUser(null);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  // Forgot password
  const forgotPassword = useCallback(async (email: string) => {
    setIsLoading(true);
    try {
      await authApi.forgotPassword(email);
    } catch (error: any) {
      throw new Error(handleApiError(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Reset password
  const resetPassword = useCallback(async (token: string, password: string) => {
    setIsLoading(true);
    try {
      await authApi.resetPassword(token, password);
    } catch (error: any) {
      throw new Error(handleApiError(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Verify email
  const verifyEmail = useCallback(async (token: string) => {
    setIsLoading(true);
    try {
      await authApi.verifyEmail(token);
      // Update user verification status if currently logged in
      if (user) {
        updateProfile({ ...user, emailVerified: true } as any);
      }
    } catch (error: any) {
      throw new Error(handleApiError(error));
    } finally {
      setIsLoading(false);
    }
  }, [user, updateProfile]);

  // Resend verification email
  const resendVerification = useCallback(async (email: string) => {
    setIsLoading(true);
    try {
      await authApi.resendVerification(email);
    } catch (error: any) {
      throw new Error(handleApiError(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    user,
    isAuthenticated,
    isLoading: isLoading || isInitializing,
    login,
    register,
    logout,
    refreshToken,
    updateProfile,
    forgotPassword,
    resetPassword,
    verifyEmail,
    resendVerification,
  };
};