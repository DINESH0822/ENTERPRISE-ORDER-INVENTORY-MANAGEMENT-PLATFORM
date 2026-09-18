import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Package,
  Building2,
  Boxes,
  ShoppingCart,
  ShoppingBag,
  UserCheck,
  Layers,
} from 'lucide-react';

export const Sidebar = () => {
  const { user } = useAuth();
  const role = user?.role;

  const isAdminOrManager = role === 'ADMIN' || role === 'WAREHOUSE_MANAGER';
  const isStaff = isAdminOrManager || role === 'SUPPORT_AGENT';

  return (
    <aside className="sidebar">
      <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <div style={{ width: 36, height: 36, borderRadius: 'var(--radius-md)', background: 'linear-gradient(135deg, #6366f1, #4f46e5)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
          <Layers size={22} />
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--text-primary)', lineHeight: '1.2' }}>ENT-PLATFORM</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Order & Inventory v1.0</div>
        </div>
      </div>

      <nav style={{ padding: '16px 12px', flex: 1, display: 'flex', flexDirection: 'column', gap: '4px' }}>
        <NavLink
          to="/dashboard"
          className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
          style={{ justifyContent: 'flex-start', padding: '10px 14px' }}
        >
          <LayoutDashboard size={18} /> Dashboard
        </NavLink>

        <NavLink
          to="/products"
          className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
          style={{ justifyContent: 'flex-start', padding: '10px 14px' }}
        >
          <Package size={18} /> Product Catalog
        </NavLink>

        {isAdminOrManager && (
          <NavLink
            to="/warehouses"
            className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
            style={{ justifyContent: 'flex-start', padding: '10px 14px' }}
          >
            <Building2 size={18} /> Warehouses
          </NavLink>
        )}

        {isAdminOrManager && (
          <NavLink
            to="/inventory"
            className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
            style={{ justifyContent: 'flex-start', padding: '10px 14px' }}
          >
            <Boxes size={18} /> Multi-Warehouse Stock
          </NavLink>
        )}

        {isStaff && (
          <NavLink
            to="/orders"
            className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
            style={{ justifyContent: 'flex-start', padding: '10px 14px' }}
          >
            <ShoppingCart size={18} /> Enterprise Orders
          </NavLink>
        )}

        {role === 'CUSTOMER' && (
          <NavLink
            to="/my-orders"
            className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
            style={{ justifyContent: 'flex-start', padding: '10px 14px' }}
          >
            <ShoppingBag size={18} /> My Orders
          </NavLink>
        )}

        <NavLink
          to="/profile"
          className={({ isActive }) => `btn btn-secondary ${isActive ? 'btn-primary' : ''}`}
          style={{ justifyContent: 'flex-start', padding: '10px 14px', marginTop: 'auto' }}
        >
          <UserCheck size={18} /> My Profile
        </NavLink>
      </nav>
    </aside>
  );
};
