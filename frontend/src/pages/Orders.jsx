import React, { useEffect, useState } from 'react';
import { orderApi } from '../api/orderApi';
import { Badge } from '../components/common/Badge';
import { Pagination } from '../components/common/Pagination';
import { LoadingPage } from '../components/common/Spinner';
import { useAuth } from '../context/AuthContext';
import { Modal } from '../components/common/Modal';

export const Orders = () => {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [statusModal, setStatusModal] = useState({ open: false, orderId: null });
  const [newStatus, setNewStatus] = useState('');

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const params = { page, size: 10 };
      const data = await orderApi.getAllOrders(params);
      setOrders(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load orders', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const openStatusModal = (orderId) => {
    setStatusModal({ open: true, orderId });
    setNewStatus('');
  };

  const closeStatusModal = () => setStatusModal({ open: false, orderId: null });

  const handleStatusUpdate = async () => {
    if (!statusModal.orderId || !newStatus) return;
    try {
      await orderApi.updateOrderStatus(statusModal.orderId, { status: newStatus });
      await fetchOrders();
      closeStatusModal();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  if (loading) return <LoadingPage />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Enterprise Orders</h1>
          <p className="page-subtitle">View and manage all platform orders</p>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer</th>
              <th>Status</th>
              <th>Total</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.customer?.username || '-'}</td>
                <td><Badge status={o.status} /></td>
                <td>{o.total?.toFixed(2)}</td>
                <td>{new Date(o.createdAt).toLocaleDateString()}</td>
                <td>
                  {user?.role === 'ADMIN' && (
                    <button className="btn btn-sm btn-secondary" onClick={() => openStatusModal(o.id)}>
                      Change Status
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pagination
        currentPage={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
      />

      {/* Status Update Modal */}
      <Modal
        isOpen={statusModal.open}
        onClose={closeStatusModal}
        title="Update Order Status"
        footer={
          <>
            <button className="btn btn-secondary" onClick={closeStatusModal}>Cancel</button>
            <button className="btn btn-primary" onClick={handleStatusUpdate}>Save</button>
          </>
        }
      >
        <div className="form-group">
          <label className="form-label">New Status</label>
          <select className="form-select" value={newStatus} onChange={(e) => setNewStatus(e.target.value)}>
            <option value="">Select status</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="PROCESSING">PROCESSING</option>
            <option value="SHIPPED">SHIPPED</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </div>
      </Modal>
    </div>
  );
};
