import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield } from 'lucide-react';

export const Profile = () => {
  const { user, logout } = useAuth();

  if (!user) {
    return null; // AuthProvider will redirect if not authenticated
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Profile</h1>
          <p className="page-subtitle">Account details and settings</p>
        </div>
      </div>

      <div className="glass-card" style={{ maxWidth: '480px', margin: '0 auto', padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div style={{ width: 48, height: 48, borderRadius: 'var(--radius-md)', background: 'linear-gradient(135deg, #6366f1, #4f46e5)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
            <Shield size={28} />
          </div>
          <div>
            <h2 style={{ margin: 0, color: 'var(--text-primary)' }}>{user.fullName || user.username}</h2>
            <p style={{ margin: 0, color: 'var(--text-secondary)' }}>{user.email || 'No email provided'}</p>
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Username</label>
          <input type="text" className="form-input" value={user.username} readOnly />
        </div>
        <div className="form-group">
          <label className="form-label">Role</label>
          <input type="text" className="form-input" value={user.role} readOnly />
        </div>

        <button className="btn btn-danger" onClick={logout} style={{ marginTop: '20px' }}>
          Sign Out
        </button>
      </div>
    </div>
  );
};
