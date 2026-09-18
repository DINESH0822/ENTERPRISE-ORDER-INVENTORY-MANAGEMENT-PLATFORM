import axiosClient from './axiosClient';

export const productApi = {
  getProducts: async (params = {}) => {
    // params: { search, categoryId, activeOnly, page, size, sortBy, sortDir }
    const response = await axiosClient.get('/products', { params });
    return response.data;
  },

  getProductById: async (id) => {
    const response = await axiosClient.get(`/products/${id}`);
    return response.data;
  },

  getProductBySku: async (sku) => {
    const response = await axiosClient.get(`/products/sku/${sku}`);
    return response.data;
  },

  createProduct: async (productData) => {
    const response = await axiosClient.post('/products', productData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const response = await axiosClient.put(`/products/${id}`, productData);
    return response.data;
  },

  deleteProduct: async (id) => {
    const response = await axiosClient.delete(`/products/${id}`);
    return response.data;
  },
};
