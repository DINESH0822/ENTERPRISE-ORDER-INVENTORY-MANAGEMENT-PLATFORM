import axiosClient from './axiosClient';

export const categoryApi = {
  getCategories: async () => {
    const response = await axiosClient.get('/categories');
    return response.data;
  },

  getCategoryById: async (id) => {
    const response = await axiosClient.get(`/categories/${id}`);
    return response.data;
  },

  createCategory: async (categoryData) => {
    const response = await axiosClient.post('/categories', categoryData);
    return response.data;
  },

  updateCategory: async (id, categoryData) => {
    const response = await axiosClient.put(`/categories/${id}`, categoryData);
    return response.data;
  },

  deleteCategory: async (id) => {
    const response = await axiosClient.delete(`/categories/${id}`);
    return response.data;
  },
};
