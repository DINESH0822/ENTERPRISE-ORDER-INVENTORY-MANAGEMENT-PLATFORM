import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { analyticsApi } from '../api/analyticsApi';
import { productApi } from '../api/productApi';
import { orderApi } from '../api/orderApi';
import {
  Package,
  Building2,
  ShoppingCart,
  Shield,
  ArrowRight,
  Boxes,
  TrendingUp,
  DollarSign,
  AlertTriangle,
  Download,
  FileSpreadsheet,
  RefreshCw,
  FileText
} from 'lucide-react';
import { LoadingPage } from '../components/common/Spinner';

export const Dashboard = () => {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState(null);
  const [customerStats, setCustomerStats] = useState({ productsCount: 0, myOrdersCount: 0 });
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const role = user?.role || (user?.roles && user.roles[0]) || 'CUSTOMER';
  const isStaff = role === 'ADMIN' || role === 'WAREHOUSE_MANAGER' || role === 'SUPPORT_AGENT';

  const loadData = async () => {
    try {
      setLoading(true);
      if (isStaff) {
        const data = await analyticsApi.getDashboardAnalytics();
        setAnalytics(data);
      } else {
        const [prodRes, myOrders] = await Promise.all([
          productApi.getProducts({ page: 0, size: 1 }).catch(() => ({ totalElements: 0 })),
          orderApi.getMyOrders(0, 1).catch(() => ({ totalElements: 0 }))
        ]);
        setCustomerStats({
          productsCount: prodRes.totalElements || 0,
          myOrdersCount: myOrders.totalElements || 0
        });
      }
    } catch (err) {
      console.error('Error loading dashboard analytics data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [role, isStaff]);

  const handleExport = async (exportFn) => {
    try {
      setExporting(true);
      await exportFn();
    } catch (err) {
      console.error('CSV Export failed', err);
      alert('Failed to generate CSV export. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  if (loading) return <LoadingPage />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="page-title">Enterprise Analytics Dashboard</h1>
          <p className="page-subtitle">Real-time order orchestration, multi-warehouse stock ledger & report hub</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button onClick={loadData} className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
            <RefreshCw size={14} /> Refresh Metrics
          </button>
          <span className="badge badge-success">
            <TrendingUp size={14} /> System Operational
          </span>
        </div>
      </div>

      {isStaff && analytics ? (
        <>
          {/* Main KPI Stat Cards */}
          <div className="grid-stats">
            <div className="stat-card">
              <div>
                <div className="stat-title">Total Revenue</div>
                <div className="stat-value" style={{ color: 'var(--success, #10b981)' }}>
                  ${Number(analytics.totalRevenue || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
              </div>
              <div className="stat-icon" style={{ backgroundColor: 'rgba(16, 185, 129, 0.15)', color: '#10b981' }}>
                <DollarSign size={24} />
              </div>
            </div>

            <div className="stat-card">
              <div>
                <div className="stat-title">Total Platform Orders</div>
                <div className="stat-value">{analytics.totalOrders}</div>
              </div>
              <div className="stat-icon" style={{ backgroundColor: 'rgba(59, 130, 246, 0.15)', color: '#3b82f6' }}>
                <ShoppingCart size={24} />
              </div>
            </div>

            <div className="stat-card">
              <div>
                <div className="stat-title">Low Stock Alerts</div>
                <div className="stat-value" style={{ color: analytics.lowStockCount > 0 ? '#f59e0b' : 'inherit' }}>
                  {analytics.lowStockCount}
                </div>
              </div>
              <div className="stat-icon" style={{ backgroundColor: analytics.lowStockCount > 0 ? 'rgba(245, 158, 11, 0.15)' : 'rgba(107, 114, 128, 0.15)', color: analytics.lowStockCount > 0 ? '#f59e0b' : '#6b7280' }}>
                <AlertTriangle size={24} />
              </div>
            </div>

            <div className="stat-card">
              <div>
                <div className="stat-title">Catalog Products</div>
                <div className="stat-value">{analytics.totalProducts}</div>
              </div>
              <div className="stat-icon">
                <Package size={24} />
              </div>
            </div>

            <div className="stat-card">
              <div>
                <div className="stat-title">Active Warehouses</div>
                <div className="stat-value">{analytics.totalWarehouses}</div>
              </div>
              <div className="stat-icon" style={{ backgroundColor: 'rgba(139, 92, 246, 0.15)', color: '#8b5cf6' }}>
                <Building2 size={24} />
              </div>
            </div>
          </div>

          {/* Report CSV Export Bar */}
          <div className="glass-card" style={{ marginTop: '24px', padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <FileSpreadsheet size={22} color="var(--accent-primary)" />
              <div>
                <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 600 }}>Quick Report CSV Exports</h4>
                <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Download real-time comma-separated reports for low stock items, orders, or full inventory ledger.</p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <button
                disabled={exporting}
                onClick={() => handleExport(analyticsApi.downloadLowStockCsv)}
                className="btn btn-secondary btn-sm"
                style={{ gap: '6px', color: '#f59e0b', borderColor: 'rgba(245, 158, 11, 0.3)' }}
              >
                <Download size={14} /> Low Stock CSV
              </button>
              <button
                disabled={exporting}
                onClick={() => handleExport(analyticsApi.downloadOrdersCsv)}
                className="btn btn-secondary btn-sm"
                style={{ gap: '6px' }}
              >
                <FileText size={14} /> Orders CSV
              </button>
              <button
                disabled={exporting}
                onClick={() => handleExport(analyticsApi.downloadInventoryCsv)}
                className="btn btn-secondary btn-sm"
                style={{ gap: '6px' }}
              >
                <Download size={14} /> Full Inventory CSV
              </button>
            </div>
          </div>

          {/* Middle Row: Order Status Breakdown & Urgent Low Stock Preview */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '20px', marginTop: '24px' }}>
            
            {/* Order Status Distribution */}
            <div className="glass-card">
              <h3 style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShoppingCart size={20} color="var(--accent-primary)" /> Order Lifecycle Breakdown
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                {Object.entries(analytics.ordersByStatus || {}).map(([status, count]) => {
                  const pct = analytics.totalOrders > 0 ? ((count / analytics.totalOrders) * 100).toFixed(0) : 0;
                  const statusColors = {
                    PENDING: '#f59e0b',
                    CONFIRMED: '#3b82f6',
                    PROCESSING: '#8b5cf6',
                    SHIPPED: '#06b6d4',
                    DELIVERED: '#10b981',
                    CANCELLED: '#ef4444',
                  };
                  const color = statusColors[status] || '#6b7280';
                  return (
                    <div key={status}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{status}</span>
                        <span style={{ color: 'var(--text-secondary)' }}>{count} ({pct}%)</span>
                      </div>
                      <div style={{ height: '8px', width: '100%', backgroundColor: 'rgba(255, 255, 255, 0.08)', borderRadius: '4px', overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${pct}%`, backgroundColor: color, borderRadius: '4px', transition: 'width 0.4s ease' }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Low Stock Alerts Preview Table */}
            <div className="glass-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <AlertTriangle size={20} color="#f59e0b" /> Urgent Low Stock Items
                </h3>
                <Link to="/inventory" className="btn btn-secondary btn-sm" style={{ padding: '4px 10px', fontSize: '0.75rem' }}>
                  View All <ArrowRight size={12} />
                </Link>
              </div>

              {analytics.lowStockPreview && analytics.lowStockPreview.length > 0 ? (
                <div style={{ overflowX: 'auto' }}>
                  <table className="table" style={{ fontSize: '0.82rem' }}>
                    <thead>
                      <tr>
                        <th>SKU</th>
                        <th>Product</th>
                        <th>Warehouse</th>
                        <th style={{ textAlign: 'right' }}>Avail / Reorder</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analytics.lowStockPreview.map((item) => (
                        <tr key={item.id}>
                          <td><code>{item.productSku}</code></td>
                          <td style={{ fontWeight: 500 }}>{item.productName}</td>
                          <td><span className="badge badge-info">{item.warehouseCode}</span></td>
                          <td style={{ textAlign: 'right', fontWeight: 600, color: '#ef4444' }}>
                            {item.availableQuantity} / {item.reorderLevel}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                  <TrendingUp size={36} color="var(--success)" style={{ marginBottom: '8px', opacity: 0.8 }} />
                  <p style={{ margin: 0 }}>All inventory items are currently above reorder thresholds!</p>
                </div>
              )}
            </div>
          </div>
        </>
      ) : (
        /* Customer Dashboard View */
        <div className="grid-stats">
          <div className="stat-card">
            <div>
              <div className="stat-title">Catalog Products</div>
              <div className="stat-value">{customerStats.productsCount}</div>
            </div>
            <div className="stat-icon">
              <Package size={24} />
            </div>
          </div>

          <div className="stat-card">
            <div>
              <div className="stat-title">My Orders</div>
              <div className="stat-value">{customerStats.myOrdersCount}</div>
            </div>
            <div className="stat-icon" style={{ backgroundColor: 'rgba(59, 130, 246, 0.15)', color: '#3b82f6' }}>
              <ShoppingCart size={24} />
            </div>
          </div>

          <div className="stat-card">
            <div>
              <div className="stat-title">Account Role</div>
              <div className="stat-value" style={{ fontSize: '1.1rem', textTransform: 'uppercase', color: 'var(--accent-primary)' }}>
                {role}
              </div>
            </div>
            <div className="stat-icon" style={{ backgroundColor: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b' }}>
              <Shield size={24} />
            </div>
          </div>
        </div>
      )}

      {/* Quick Navigation Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px', marginTop: '24px' }}>
        <div className="glass-card">
          <h3 style={{ marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Package size={20} color="var(--accent-primary)" /> Product Catalog Actions
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '16px' }}>
            Browse products, search by SKU or name, filter by category & manage product entries.
          </p>
          <Link to="/products" className="btn btn-secondary btn-sm">
            Manage Catalog <ArrowRight size={14} />
          </Link>
        </div>

        {isStaff && (
          <div className="glass-card">
            <h3 style={{ marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Boxes size={20} color="var(--success)" /> Stock & Ledger Audit
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '16px' }}>
              Perform stock receiving, issuing, adjustment, low stock detection, and inspect immutable audit transactions.
            </p>
            <Link to="/inventory" className="btn btn-secondary btn-sm">
              Open Stock Ledger <ArrowRight size={14} />
            </Link>
          </div>
        )}

        {isStaff ? (
          <div className="glass-card">
            <h3 style={{ marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShoppingCart size={20} color="var(--info)" /> Order Lifecycle Center
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '16px' }}>
              Process incoming customer orders, manage state transitions (`CONFIRMED`, `SHIPPED`, `DELIVERED`), and review order audit trails.
            </p>
            <Link to="/orders" className="btn btn-secondary btn-sm">
              View All Orders <ArrowRight size={14} />
            </Link>
          </div>
        ) : (
          <div className="glass-card">
            <h3 style={{ marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShoppingCart size={20} color="var(--info)" /> Order History & Placement
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '16px' }}>
              Place new inventory orders, view order status updates, and manage order cancellations.
            </p>
            <Link to="/my-orders" className="btn btn-secondary btn-sm">
              View My Orders <ArrowRight size={14} />
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
