import React from 'react';

export const Badge = ({ status, text }) => {
  const displayValue = text || status;
  let badgeClass = 'badge-secondary';

  switch (status?.toUpperCase()) {
    case 'CONFIRMED':
    case 'DELIVERED':
    case 'ACTIVE':
    case 'IN_STOCK':
    case 'RECEIVE':
      badgeClass = 'badge-success';
      break;

    case 'PENDING':
    case 'PROCESSING':
    case 'LOW_STOCK':
    case 'ADJUSTMENT':
      badgeClass = 'badge-warning';
      break;

    case 'CANCELLED':
    case 'INACTIVE':
    case 'OUT_OF_STOCK':
    case 'ISSUE':
      badgeClass = 'badge-danger';
      break;

    case 'SHIPPED':
    case 'RESERVED':
    case 'RESERVE':
    case 'RELEASE':
      badgeClass = 'badge-info';
      break;

    default:
      badgeClass = 'badge-secondary';
  }

  return <span className={`badge ${badgeClass}`}>{displayValue}</span>;
};
