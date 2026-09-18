import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/common/Toast';
import { Layers, UserPlus } from 'lucide-react';

export const Register = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phone: '',
  });
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.firstName || !formData.lastName) {
      addToast('Please provide both first and last name', 'warning');
      return;
    }
    if (!formData.email || !formData.email.includes('@') || !formData.email.includes('.')) {
      addToast('Please provide a valid email (e.g. user@example.com)', 'warning');
      return;
    }
    if (formData.password.length < 6) {
      addToast('Password must be at least 6 characters', 'warning');
      return;
    }

    try {
      setLoading(true);
      await register(formData);
      addToast('Account created successfully! Please sign in.', 'success');
      navigate('/login');
    } catch (err) {
      console.error(err);
      const errors = err.response?.data?.errors;
      let msg = err.response?.data?.message || err.response?.data?.error;
      if (errors && typeof errors === 'object') {
        msg = Object.values(errors).join(', ');
      }
      addToast(msg || 'Registration failed. Please check your inputs.', 'danger');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--bg-primary)', padding: '20px' }}>
      <div className="glass-card" style={{ width: '100%', maxWidth: '480px', padding: '36px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{ width: 48, height: 48, borderRadius: 'var(--radius-md)', background: 'linear-gradient(135deg, #6366f1, #4f46e5)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', color: '#fff', marginBottom: '12px' }}>
            <Layers size={28} />
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700 }}>Create Enterprise Account</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>Register to manage catalog, orders & inventory</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="form-group">
              <label className="form-label">First Name</label>
              <input
                type="text"
                name="firstName"
                className="form-input"
                placeholder="e.g. Dinesh"
                value={formData.firstName}
                onChange={handleChange}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Last Name</label>
              <input
                type="text"
                name="lastName"
                className="form-input"
                placeholder="e.g. Kumar"
                value={formData.lastName}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email"
              name="email"
              className="form-input"
              placeholder="e.g. dinesh@example.com"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              name="password"
              className="form-input"
              placeholder="Minimum 6 characters"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Phone (Optional)</label>
            <input
              type="tel"
              name="phone"
              className="form-input"
              placeholder="e.g. +91 9876543210"
              value={formData.phone}
              onChange={handleChange}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '16px', padding: '12px' }}
            disabled={loading}
          >
            {loading ? 'Creating Account...' : 'Register Account'} <UserPlus size={18} />
          </button>
        </form>

        <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Already registered?{' '}
          <Link to="/login" style={{ color: 'var(--accent-primary)', fontWeight: 600 }}>
            Sign In Here
          </Link>
        </div>
      </div>
    </div>
  );
};
