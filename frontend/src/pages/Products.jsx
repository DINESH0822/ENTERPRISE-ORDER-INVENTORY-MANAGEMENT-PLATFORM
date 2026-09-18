import React, { useEffect, useState } from 'react';
import { productApi } from '../api/productApi';
import { orderApi } from '../api/orderApi';
import { Badge } from '../components/common/Badge';
import { Pagination } from '../components/common/Pagination';
import { LoadingPage } from '../components/common/Spinner';
import { Modal } from '../components/common/Modal';
import { useToast } from '../components/common/Toast';
import { ShoppingCart, Search } from 'lucide-react';

export const Products = () => {
  const { addToast } = useToast();
  const [products, setProducts] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  
  // Order Modal State
  const [orderModal, setOrderModal] = useState({ open: false, product: null });
  const [orderQuantity, setOrderQuantity] = useState(1);
  const [shippingAddress, setShippingAddress] = useState('456 Technology Way, Suite 100, San Francisco, CA');
  const [submittingOrder, setSubmittingOrder] = useState(false);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const params = {
        name: searchTerm || undefined,
        page,
        size: 10,
        sortBy: 'name',
        sortDir: 'asc',
      };
      const data = await productApi.getProducts(params);
      setProducts(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchProducts();
  };

  const handleOpenOrder = (product) => {
    setOrderModal({ open: true, product });
    setOrderQuantity(1);
  };

  const handlePlaceOrder = async () => {
    if (!orderModal.product || orderQuantity < 1) return;
    try {
      setSubmittingOrder(true);
      const payload = {
        items: [
          {
            productId: orderModal.product.id,
            warehouseId: 1, // Default Central Warehouse
            quantity: Number(orderQuantity),
          },
        ],
        shippingAddress,
      };
      await orderApi.createOrder(payload);
      addToast('Order placed successfully! Inventory reserved.', 'success');
      setOrderModal({ open: false, product: null });
    } catch (err) {
      console.error('Failed to place order', err);
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to place order';
      addToast(msg, 'danger');
    } finally {
      setSubmittingOrder(false);
    }
  };

  if (loading && products.length === 0) return <LoadingPage />;

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="page-title">Product Catalog</h1>
          <p className="page-subtitle">Manage inventory items, search, filter and place customer orders</p>
        </div>

        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '8px' }}>
          <div style={{ position: 'relative' }}>
            <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '36px', width: '240px' }}
              placeholder="Search products..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-secondary">Search</button>
        </form>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>Product Name</th>
              <th>Category</th>
              <th>Status</th>
              <th>Unit Price</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {products.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: 'var(--text-secondary)' }}>
                  No products found.
                </td>
              </tr>
            ) : (
              products.map((p) => (
                <tr key={p.id}>
                  <td style={{ fontWeight: 600, color: 'var(--accent-primary)' }}>{p.sku}</td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{p.description}</div>
                  </td>
                  <td>{p.category?.name || '-'}</td>
                  <td><Badge status={p.status || 'ACTIVE'} /></td>
                  <td style={{ fontWeight: 700 }}>${(p.price ?? 0).toFixed(2)}</td>
                  <td>
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => handleOpenOrder(p)}
                      style={{ gap: '6px' }}
                    >
                      <ShoppingCart size={14} /> Place Order
                    </button>
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

      {/* Place Order Modal */}
      <Modal
        isOpen={orderModal.open}
        onClose={() => setOrderModal({ open: false, product: null })}
        title={`Place Order: ${orderModal.product?.name || ''}`}
        footer={
          <>
            <button
              className="btn btn-secondary"
              onClick={() => setOrderModal({ open: false, product: null })}
              disabled={submittingOrder}
            >
              Cancel
            </button>
            <button
              className="btn btn-primary"
              onClick={handlePlaceOrder}
              disabled={submittingOrder}
            >
              {submittingOrder ? 'Placing Order...' : 'Confirm & Place Order'}
            </button>
          </>
        }
      >
        {orderModal.product && (
          <div>
            <div style={{ padding: '12px', background: 'rgba(255,255,255,0.04)', borderRadius: 'var(--radius-md)', marginBottom: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Unit Price:</span>
                <span style={{ fontWeight: 700 }}>${orderModal.product.price?.toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Quantity:</span>
                <span>{orderQuantity} unit(s)</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border-color)', paddingTop: '6px' }}>
                <span style={{ fontWeight: 600 }}>Estimated Subtotal:</span>
                <span style={{ fontWeight: 700, color: 'var(--success)' }}>
                  ${(orderModal.product.price * orderQuantity).toFixed(2)}
                </span>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Order Quantity</label>
              <input
                type="number"
                min="1"
                max="100"
                className="form-input"
                value={orderQuantity}
                onChange={(e) => setOrderQuantity(Math.max(1, parseInt(e.target.value) || 1))}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Shipping Address</label>
              <textarea
                className="form-input"
                rows="3"
                value={shippingAddress}
                onChange={(e) => setShippingAddress(e.target.value)}
                placeholder="Enter complete shipping destination..."
                required
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
