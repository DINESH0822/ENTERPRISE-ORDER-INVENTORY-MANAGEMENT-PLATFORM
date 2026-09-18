import React, { useEffect, useState } from 'react';
import { orderApi } from '../api/orderApi';
import { Badge } from '../components/common/Badge';
import { Pagination } from '../components/common/Pagination';
import { LoadingPage } from '../components/common/Spinner';
export const MyOrders = () => {
  const [orders, setOrders] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const fetchMyOrders = async () => {
    setLoading(true);
    try {
      const data = await orderApi.getMyOrders(page, 10);
      setOrders(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load my orders', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  if (loading) return <LoadingPage />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Orders</h1>
          <p className="page-subtitle">Your order history and status</p>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Order Number</th>
              <th>Status</th>
              <th>Total Amount</th>
              <th>Placed At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.length === 0 ? (
              <tr>
                <td colSpan="5" style={{ textAlign: 'center', padding: '32px', color: 'var(--text-secondary)' }}>
                  No orders placed yet. Browse the Product Catalog to place an order!
                </td>
              </tr>
            ) : (
              orders.map((o) => (
                <tr key={o.id}>
                  <td style={{ fontWeight: 600, color: 'var(--accent-primary)' }}>{o.orderNumber || `#${o.id}`}</td>
                  <td><Badge status={o.status} /></td>
                  <td style={{ fontWeight: 600 }}>${(o.totalAmount ?? o.total ?? 0).toFixed(2)}</td>
                  <td>{o.createdAt ? new Date(o.createdAt).toLocaleString() : '-'}</td>
                  <td>
                    {(o.status === 'PENDING' || o.status === 'CONFIRMED') && (
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={async () => {
                          if (window.confirm('Are you sure you want to cancel this order?')) {
                            try {
                              await orderApi.cancelOrder(o.id, { reason: 'Customer requested cancellation' });
                              fetchMyOrders();
                            } catch (e) {
                              console.error('Failed to cancel order', e);
                            }
                          }
                        }}
                      >
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))
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
    </div>
  );
};
