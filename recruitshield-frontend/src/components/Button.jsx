import React from 'react';
import { Link } from 'react-router-dom';

export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  icon,
  iconRight,
  to,
  href,
  onClick,
  className = '',
  disabled = false,
  loading = false,
  type = 'button',
  ...props
}) {
  const sizeClasses = {
    sm: 'px-3.5 py-1.5 text-xs rounded-xl gap-1.5',
    md: 'px-5 py-2.5 text-sm rounded-xl gap-2',
    lg: 'px-6 py-3 text-[18px] rounded-xl gap-2.5',
    xl: 'px-8 py-3.5 text-xl rounded-xl gap-3'
  }[size] || 'px-6 py-3 text-[18px] rounded-xl gap-2.5';

  const variantClasses = {
    primary:
      'bg-gradient-to-r from-[#0891b2] to-[#1d4ed8] text-white font-bold shadow-[0_2px_14px_rgba(6,182,212,0.25)] hover:brightness-110 active:scale-[0.98]',
    secondary:
      'bg-surface-container-high hover:bg-surface-bright text-on-surface font-semibold shadow-sm border border-surface-variant/40 active:scale-[0.98]',
    outline:
      'bg-transparent hover:bg-surface-container text-on-surface border border-surface-variant hover:border-primary/50',
    ghost:
      'bg-transparent hover:bg-surface-container text-on-surface-variant hover:text-on-surface',
    destructive:
      'bg-error-container/30 hover:bg-error-container/50 text-error border border-error/30 active:scale-[0.98]'
  }[variant] || 'bg-surface-container text-on-surface';

  const isPrimary = variant === 'primary';

  const baseClasses = `inline-flex items-center justify-center transition-all duration-150 select-none ${sizeClasses} ${variantClasses} ${
    disabled || loading ? 'opacity-50 pointer-events-none cursor-not-allowed' : 'cursor-pointer'
  } ${className}`;

  const iconClasses = `material-symbols-outlined text-[20px] shrink-0 ${isPrimary ? 'text-white' : ''}`;

  const content = (
    <>
      {loading ? (
        <span className={`${iconClasses} animate-spin`}>sync</span>
      ) : icon ? (
        <span className={iconClasses}>{icon}</span>
      ) : null}
      <span className={isPrimary ? 'text-white font-bold' : ''}>{children}</span>
      {!loading && iconRight ? (
        <span className={iconClasses}>{iconRight}</span>
      ) : null}
    </>
  );

  if (to) {
    return (
      <Link to={to} className={baseClasses} {...props}>
        {content}
      </Link>
    );
  }

  if (href) {
    return (
      <a href={href} className={baseClasses} {...props}>
        {content}
      </a>
    );
  }

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={baseClasses}
      {...props}
    >
      {content}
    </button>
  );
}
