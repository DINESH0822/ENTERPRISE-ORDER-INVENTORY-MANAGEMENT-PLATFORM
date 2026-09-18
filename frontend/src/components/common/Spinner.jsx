import React from 'react';

export const Spinner = ({ size = 24, style }) => {
  return (
    <div
      className="spinner"
      style={{
        width: size,
        height: size,
        ...style,
      }}
    />
  );
};

export const LoadingPage = () => (
  <div style={{ display: 'flex', minHeight: '300px', alignItems: 'center', justifyContent: 'center' }}>
    <Spinner size={36} />
  </div>
);
