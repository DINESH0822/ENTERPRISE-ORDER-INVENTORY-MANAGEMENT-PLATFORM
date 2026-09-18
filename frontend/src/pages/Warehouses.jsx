import React, { useEffect, useState, useCallback } from 'react';
import { warehouseApi } from '../api/warehouseApi';
import { Badge } from '../components/common/Badge';
import { Pagination } from '../components/common/Pagination';
import { LoadingPage } from '../components/common/Spinner';
import { Modal } from '../components/common/Modal';
import { useToast } from '../components/common/Toast';
import { Building2, Plus, Power, MapPin, RefreshCw } from 'lucide-react';

export const Warehouses = () => {
  const { addToast } = useToast();
  const [warehouses, setWarehouses] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Create Warehouse Modal State
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    address: '',
    city: '',
    state: '',
    country: 'USA',
    postalCode: '',
    status: 'ACTIVE',
  });

  const fetchWarehouses = useCallback(async () => {
    setLoading(true);
    try {
      const data = await warehouseApi.getWarehouses({ page, size: 10 });
      if (Array.isArray(data)) {
        setWarehouses(data);
        setTotalElements(data.length);
        setTotalPages(1);
      } else {
        setWarehouses(data.content || []);
        setTotalElements(data.totalElements || 0);
        setTotalPages(data.totalPages || 0);
      }
    } catch (err) {
      console.error('Failed to load warehouses', err);
      addToast('Failed to load warehouses', 'danger');
    } finally {
      setLoading(false);
    }
  }, [page, addToast]);

  useEffect(() => {
    fetchWarehouses();
  }, [fetchWarehouses]);

  const handleToggleStatus = async (warehouse) => {
    const newStatus = warehouse.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setActionLoadingId(warehouse.id);
    try {
      await warehouseApi.updateWarehouseStatus(warehouse.id, newStatus);
      addToast(`Warehouse ${warehouse.name} marked as ${newStatus}`, 'success');
      fetchWarehouses();
    } catch (err) {
      console.error('Failed to toggle warehouse status', err);
      addToast(err?.response?.data?.message || 'Failed to update status', 'danger');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCreateWarehouse = async (e) => {
    e.preventDefault();
    if (!formData.code || !formData.name) {
      addToast('Warehouse code and name are required', 'warning');
      return;
    }
    setCreating(true);
    try {
      await warehouseApi.createWarehouse(formData);
      addToast('Warehouse registered successfully!', 'success');
      setCreateModalOpen(false);
      setFormData({
        code: '',
        name: '',
        address: '',
        city: '',
        state: '',
        country: 'USA',
        postalCode: '',
        status: 'ACTIVE',
      });
      fetchWarehouses();
    } catch (err) {
      console.error('Failed to create warehouse', err);
      addToast(err?.response?.data?.message || 'Failed to create warehouse', 'danger');
    } finally {
      setCreating(false);
    }
  };

  if (loading && warehouses.length === 0) return <LoadingPage />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Building2 size={24} color="var(--accent-primary)" /> Warehouses
          </h1>
          <p className="page-subtitle">Manage fulfillment locations, addresses, and operational statuses</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-secondary btn-sm" onClick={fetchWarehouses} title="Refresh warehouses">
            <RefreshCw size={15} /> Refresh
          </button>
          <button className="btn btn-primary btn-sm" onClick={() => setCreateModalOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Plus size={16} /> Add Warehouse
          </button>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 60 }}>ID</th>
              <th>Code</th>
              <th>Name</th>
              <th>Location / Address</th>
              <th>Status</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {warehouses.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
                  No warehouses found. Click "Add Warehouse" above to register one.
                </td>
              </tr>
            ) : (
              warehouses.map((w) => {
                const isInactive = w.status === 'INACTIVE';
                const locationText = [w.city, w.state, w.country].filter(Boolean).join(', ') || w.address || '-';
                return (
                  <tr key={w.id}>
                    <td><span style={{ fontWeight: 600, color: 'var(--text-muted)' }}>#{w.id}</span></td>
                    <td><code style={{ background: 'rgba(255,255,255,0.06)', padding: '2px 6px', borderRadius: 4 }}>{w.code || '-'}</code></td>
                    <td><span style={{ fontWeight: 600 }}>{w.name}</span></td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text-secondary)' }}>
                        <MapPin size={14} color="var(--text-muted)" />
                        <span>{locationText}</span>
                      </div>
                    </td>
                    <td>
                      <Badge status={w.status || (w.active ? 'ACTIVE' : 'INACTIVE')} />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className={`btn btn-sm ${isInactive ? 'btn-primary' : 'btn-secondary'}`}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.75rem', padding: '4px 10px' }}
                        disabled={actionLoadingId === w.id}
                        onClick={() => handleToggleStatus(w)}
                        title={isInactive ? 'Activate Warehouse' : 'Deactivate Warehouse'}
                      >
                        <Power size={13} />
                        {actionLoadingId === w.id ? 'Updating...' : (isInactive ? 'Activate' : 'Deactivate')}
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <Pagination
        currentPage={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
      />

      {/* Create Warehouse Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Add New Fulfillment Warehouse"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setCreateModalOpen(false)} disabled={creating}>
              Cancel
            </button>
            <button className="btn btn-primary" onClick={handleCreateWarehouse} disabled={creating}>
              {creating ? 'Creating...' : 'Register Warehouse'}
            </button>
          </>
        }
      >
        <form onSubmit={handleCreateWarehouse}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Warehouse Code *</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. WH-EAST-01"
                required
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Warehouse Name *</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. New York Logistics Hub"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Street Address</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. 100 Industrial Parkway"
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">City</label>
              <input
                type="text"
                className="form-input"
                placeholder="City"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label className="form-label">State</label>
              <input
                type="text"
                className="form-input"
                placeholder="State"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Postal Code</label>
              <input
                type="text"
                className="form-input"
                placeholder="ZIP"
                value={formData.postalCode}
                onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Country</label>
            <input
              type="text"
              className="form-input"
              value={formData.country}
              onChange={(e) => setFormData({ ...formData, country: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
};
