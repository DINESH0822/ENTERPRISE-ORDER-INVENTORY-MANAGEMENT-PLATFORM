import axiosClient from './axiosClient';

export const warehouseApi = {
  getWarehouses: async () => {
    const response = await axiosClient.get('/warehouses');
    return response.data;
  },

  getWarehouseById: async (id) => {
    const response = await axiosClient.get(`/warehouses/${id}`);
    return response.data;
  },

  createWarehouse: async (warehouseData) => {
    const response = await axiosClient.post('/warehouses', warehouseData);
    return response.data;
  },

  updateWarehouse: async (id, warehouseData) => {
    const response = await axiosClient.put(`/warehouses/${id}`, warehouseData);
    return response.data;
  },
};
