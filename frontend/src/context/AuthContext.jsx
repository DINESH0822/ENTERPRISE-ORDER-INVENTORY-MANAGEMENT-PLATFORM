import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

const normalizeUser = (profile) => {
  if (!profile) return null;
  const roles = Array.isArray(profile.roles)
    ? profile.roles
    : (profile.roles ? Array.from(profile.roles) : []);
  const cleanRoles = roles.map((r) => r.replace('ROLE_', ''));
  const primaryRole = cleanRoles[0] || (profile.role ? profile.role.replace('ROLE_', '') : 'CUSTOMER');
  return {
    ...profile,
    roles: cleanRoles,
    role: primaryRole,
  };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user_info');
    return savedUser ? normalizeUser(JSON.parse(savedUser)) : null;
  });

  const [token, setToken] = useState(() => localStorage.getItem('jwt_token'));
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('jwt_token');
      if (storedToken) {
        try {
          const profile = await authApi.getProfile();
          const normalized = normalizeUser(profile);
          setUser(normalized);
          localStorage.setItem('user_info', JSON.stringify(normalized));
        } catch (error) {
          console.error('Failed to fetch profile on load', error);
          localStorage.removeItem('jwt_token');
          localStorage.removeItem('user_info');
          setToken(null);
          setUser(null);
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = async (credentials) => {
    const data = await authApi.login(credentials);
    const authToken = data.accessToken || data.token;
    localStorage.setItem('jwt_token', authToken);
    setToken(authToken);

    let rawProfile = data.user;
    if (!rawProfile) {
      rawProfile = await authApi.getProfile();
    }
    const profile = normalizeUser(rawProfile);
    setUser(profile);
    localStorage.setItem('user_info', JSON.stringify(profile));
    return profile;
  };

  const register = async (userData) => {
    return await authApi.register(userData);
  };

  const logout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_info');
    setToken(null);
    setUser(null);
  };

  const hasRole = (roles) => {
    if (!user) return false;
    const userRoles = Array.isArray(user.roles) ? user.roles : (user.role ? [user.role] : []);
    if (Array.isArray(roles)) {
      return roles.some((r) => userRoles.includes(r) || userRoles.includes(`ROLE_${r}`));
    }
    return userRoles.includes(roles) || userRoles.includes(`ROLE_${roles}`);
  };

  const value = {
    user,
    token,
    isAuthenticated: !!token && !!user,
    isLoading,
    login,
    register,
    logout,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
