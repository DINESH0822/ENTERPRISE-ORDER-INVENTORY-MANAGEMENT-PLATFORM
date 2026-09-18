import axiosClient from './axiosClient';

export const inventoryApi = {
  getWarehouseInventory: async (warehouseId) => {
    const response = await axiosClient.get(`/inventory/warehouse/${warehouseId}`);
    return response.data;
  },

  getLowStockItems: async (warehouseId) => {
    const response = await axiosClient.get(`/inventory/warehouse/${warehouseId}/low-stock`);
    return response.data;
  },

  stockIn: async (stockInData) => {
    const response = await axiosClient.post('/inventory/stock-in', stockInData);
    return response.data;
  },

  stockOut: async (stockOutData) => {
    const response = await axiosClient.post('/inventory/stock-out', stockOutData);
    return response.data;
  },

  adjustStock: async (adjustmentData) => {
    const response = await axiosClient.post('/inventory/adjust', adjustmentData);
    return response.data;
  },

  getTransactions: async (params = {}) => {
    // params: { warehouseId, productId, page, size }
    const response = await axiosClient.get('/inventory/transactions', { params });
    return response.data;
  },
};
