import React from 'react';
import { Link } from 'react-router-dom';
import { Shield } from 'lucide-react';

export const Unauthorized = () => (
  <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--bg-primary)', padding: '20px' }}>
    <div className="glass-card" style={{ width: '100%', maxWidth: '480px', textAlign: 'center', padding: '36px' }}>
      <div style={{ marginBottom: '20px' }}>
        <Shield size={48} color="var(--danger)" />
      </div>
      <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>Access Denied</h2>
      <p style={{ color: 'var(--text-secondary)', marginTop: '12px' }}>
        You do not have permission to view this page. Please contact your administrator if you think this is an error.
      </p>
      <Link to="/dashboard" className="btn btn-primary" style={{ marginTop: '24px' }}>
        Return to Dashboard
      </Link>
    </div>
  </div>
);
