import axiosClient from './axiosClient';

export const analyticsApi = {
  getDashboardAnalytics: async () => {
    const response = await axiosClient.get('/analytics/dashboard');
    return response.data;
  },

  downloadLowStockCsv: async () => {
    const response = await axiosClient.get('/analytics/export/low-stock-csv', {
      responseType: 'blob',
    });
    triggerDownload(response.data, `low_stock_report_${new Date().toISOString().slice(0,10)}.csv`);
  },

  downloadOrdersCsv: async () => {
    const response = await axiosClient.get('/analytics/export/orders-csv', {
      responseType: 'blob',
    });
    triggerDownload(response.data, `orders_report_${new Date().toISOString().slice(0,10)}.csv`);
  },

  downloadInventoryCsv: async () => {
    const response = await axiosClient.get('/analytics/export/inventory-csv', {
      responseType: 'blob',
    });
    triggerDownload(response.data, `inventory_report_${new Date().toISOString().slice(0,10)}.csv`);
  },
};

function triggerDownload(data, filename) {
  const blob = new Blob([data], { type: 'text/csv;charset=utf-8;' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export default analyticsApi;
