import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export const Pagination = ({ currentPage, totalPages, totalElements, onPageChange }) => {
  if (totalPages <= 1) return null;

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '16px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
      <div>
        Showing page <strong style={{ color: 'var(--text-primary)' }}>{currentPage + 1}</strong> of <strong style={{ color: 'var(--text-primary)' }}>{totalPages}</strong> ({totalElements} items)
      </div>
      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          className="btn btn-secondary btn-sm"
          disabled={currentPage === 0}
          onClick={() => onPageChange(currentPage - 1)}
        >
          <ChevronLeft size={16} /> Previous
        </button>
        <button
          className="btn btn-secondary btn-sm"
          disabled={currentPage >= totalPages - 1}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Next <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
};
