import axiosClient from './axiosClient';

export const orderApi = {
  createOrder: async (orderData) => {
    const response = await axiosClient.post('/orders', orderData);
    return response.data;
  },

  getMyOrders: async (page = 0, size = 10) => {
    const response = await axiosClient.get('/orders/my-orders', {
      params: { page, size }
    });
    return response.data;
  },

  getAllOrders: async (params = {}) => {
    // params: { status, page, size }
    const response = await axiosClient.get('/orders', { params });
    return response.data;
  },

  getOrderById: async (orderId) => {
    const response = await axiosClient.get(`/orders/${orderId}`);
    return response.data;
  },

  cancelOrder: async (orderId, reason) => {
    const response = await axiosClient.post(`/orders/${orderId}/cancel`, { reason });
    return response.data;
  },

  updateOrderStatus: async (orderId, statusData) => {
    // statusData: { status, trackingNumber, carrier, notes }
    const response = await axiosClient.patch(`/orders/${orderId}/status`, statusData);
    return response.data;
  },
};
