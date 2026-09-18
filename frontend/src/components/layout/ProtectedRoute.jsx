import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingPage } from '../common/Spinner';

export const ProtectedRoute = ({ allowedRoles }) => {
  const { isAuthenticated, isLoading, hasRole } = useAuth();
  const hasStoredToken = !!localStorage.getItem('jwt_token');

  if (isLoading) {
    return <LoadingPage />;
  }

  if (!isAuthenticated && !hasStoredToken) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !hasRole(allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};
