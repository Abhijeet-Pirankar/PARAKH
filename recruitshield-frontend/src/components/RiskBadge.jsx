import React from 'react';

/**
 * Standardized status badge for PARAKH risk assessments.
 * Accepts status strings in either format:
 * - Enum: "HIGHLY_SUSPICIOUS", "NEEDS_VERIFICATION", "LIKELY_GENUINE"
 * - Title Case: "Highly Suspicious", "Needs Verification", "Likely Genuine"
 */
export default function RiskBadge({ status = 'HIGHLY_SUSPICIOUS', score = null, size = 'md', className = '' }) {
  const normalized = (status || '').toUpperCase().replace(/\s+/g, '_');

  let config = {
    label: 'Highly Suspicious',
    badgeClass: 'bg-error-container/30 text-error border-error/40',
    dotClass: 'bg-error',
    icon: 'warning'
  };

  if (normalized.includes('GENUINE') || (score !== null && score < 40)) {
    config = {
      label: 'Likely Genuine',
      badgeClass: 'bg-tertiary-container/20 text-tertiary border-tertiary/40',
      dotClass: 'bg-tertiary',
      icon: 'check_circle'
    };
  } else if (normalized.includes('VERIF') || (score !== null && score >= 40 && score < 70)) {
    config = {
      label: 'Needs Verification',
      badgeClass: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
      dotClass: 'bg-amber-400',
      icon: 'shield'
    };
  }

  const sizeClasses = {
    sm: 'text-[11px] px-2.5 py-0.5 gap-1.5',
    md: 'text-xs px-3 py-1 gap-2',
    lg: 'text-sm px-4 py-1.5 gap-2.5'
  }[size] || 'text-xs px-3 py-1 gap-2';

  return (
    <span
      className={`inline-flex items-center font-mono font-semibold uppercase tracking-wider rounded-full border ${config.badgeClass} ${sizeClasses} ${className}`}
    >
      <span className="relative flex h-2 w-2">
        <span className={`relative inline-flex rounded-full h-2 w-2 ${config.dotClass}`} />
      </span>
      <span>{config.label}</span>
    </span>
  );
}
