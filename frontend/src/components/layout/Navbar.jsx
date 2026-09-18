import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { LogOut, Shield } from 'lucide-react';

export const Navbar = () => {
  const { user, logout } = useAuth();

  return (
    <header className="top-navbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Welcome back,</span>
        <strong style={{ color: 'var(--text-primary)', fontWeight: '600' }}>{user?.fullName || user?.username}</strong>
        {user?.role && (
          <span className="badge badge-info" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Shield size={12} /> {user.role}
          </span>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <button
          onClick={logout}
          className="btn btn-secondary btn-sm"
          title="Logout of session"
        >
          <LogOut size={16} /> Logout
        </button>
      </div>
    </header>
  );
};
