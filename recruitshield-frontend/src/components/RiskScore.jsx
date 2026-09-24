import React from 'react';
import RiskBadge from './RiskBadge';

export default function RiskScore({
  score = 0,
  status = null,
  size = 'lg', // 'sm', 'md', 'lg'
  showBadge = true,
  disclaimer = true
}) {
  const normalizedScore = Math.min(Math.max(Number(score) || 0, 0), 100);

  // Determine metadata based on score and status
  const getSeverityMeta = (val) => {
    if (val >= 70) {
      return {
        label: status || 'Highly Suspicious',
        colorClass: 'text-error',
        strokeColor: '#f43f5e',
        orbColor: 'bg-error',
        severityText: 'High Risk'
      };
    }
    if (val >= 40) {
      return {
        label: status || 'Needs Verification',
        colorClass: 'text-amber-400',
        strokeColor: '#f59e0b',
        orbColor: 'bg-amber-400',
        severityText: 'Moderate Risk'
      };
    }
    return {
      label: status || 'Likely Genuine',
      colorClass: 'text-tertiary',
      strokeColor: '#4edea3',
      orbColor: 'bg-tertiary',
      severityText: 'Low Risk'
    };
  };

  const meta = getSeverityMeta(normalizedScore);

  // SVG Gauge calculations (radius = 50, circumference = 2 * PI * 50 = 314.159)
  const circumference = 314.16;
  const strokeDashoffset = circumference - (circumference * normalizedScore) / 100;

  if (size === 'sm') {
    return (
      <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-lowest border border-surface-variant/40">
        <span className={`w-2 h-2 rounded-full ${meta.orbColor}`} />
        <span className="font-mono text-xs font-bold text-on-surface">
          {normalizedScore}/100
        </span>
        <RiskBadge status={meta.label} score={normalizedScore} size="sm" />
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center relative select-none">
      {/* Status Badge */}
      {showBadge && (
        <div className="mb-3">
          <RiskBadge status={meta.label} score={normalizedScore} size="md" />
        </div>
      )}

      {/* Circular SVG Gauge */}
      <div className="relative w-48 h-48 sm:w-52 sm:h-52 flex items-center justify-center my-1">
        <svg className="w-full h-full transform -rotate-90" viewBox="0 0 120 120">
          {/* Background Track Circle */}
          <circle
            className="text-surface-container-high"
            cx="60"
            cy="60"
            r="50"
            fill="transparent"
            stroke="currentColor"
            strokeWidth="7"
            strokeDasharray={circumference}
            strokeDashoffset="0"
          />
          {/* Foreground Meter Arc */}
          <circle
            cx="60"
            cy="60"
            r="50"
            fill="transparent"
            stroke={meta.strokeColor}
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            style={{ transition: 'stroke-dashoffset 0.8s ease-in-out' }}
          />
        </svg>

        {/* Center Numbers & Description */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-4">
          <span className="font-headline text-4xl sm:text-5xl font-bold tracking-tight text-on-surface font-mono">
            {normalizedScore}
            <span className={`text-xl sm:text-2xl font-normal ${meta.colorClass}`}>/100</span>
          </span>
          <span className={`font-mono text-[11px] uppercase tracking-wider mt-1 font-semibold ${meta.colorClass}`}>
            {meta.severityText}
          </span>
          {disclaimer && (
            <span className="text-[10px] text-on-surface-variant max-w-[140px] leading-tight mt-1 opacity-75">
              AI-assisted risk assessment
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
