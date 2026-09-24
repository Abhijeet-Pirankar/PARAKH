import React from 'react';

export default function Card({
  children,
  level = 'low', // 'lowest', 'low', 'default', 'high', 'highest'
  accentTop = false,
  className = '',
  hover = false,
  padding = 'p-6',
  ...props
}) {
  const levelClasses = {
    lowest: 'bg-surface-container-lowest',
    low: 'bg-surface-container-low',
    default: 'bg-surface-container',
    high: 'bg-surface-container-high',
    highest: 'bg-surface-container-highest'
  }[level] || 'bg-surface-container-low';

  const hoverClasses = hover
    ? 'hover:bg-surface-container transition-all duration-200 hover:scale-[1.005]'
    : '';

  return (
    <div
      className={`relative rounded-xl border border-surface-variant/30 shadow-xl overflow-hidden ${levelClasses} ${padding} ${hoverClasses} ${className}`}
      {...props}
    >
      {accentTop && (
        <div className="absolute top-0 inset-x-0 h-1 bg-gradient-to-r from-primary-container via-primary to-secondary-container" />
      )}
      {children}
    </div>
  );
}
