import React from 'react';

export default function Logo({ className = "h-8 w-auto", showSubtitle = false, subtitle = "Verify Before You Trust" }) {
  return (
    <div className="flex items-center gap-3 shrink-0 select-none">
      <svg
        className={className}
        viewBox="0 0 240 64"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="shieldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#22D3EE" />
            <stop offset="100%" stopColor="#6366F1" />
          </linearGradient>
          <linearGradient id="textGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#FFFFFF" />
            <stop offset="100%" stopColor="#E2E8F0" />
          </linearGradient>
        </defs>
        {/* Shield Icon with Check/Search motif */}
        <g transform="translate(4, 8)">
          <path
            d="M24 2L6 10v14c0 12.5 7.8 24.2 18 27.5 10.2-3.3 18-15 18-27.5V10L24 2z"
            fill="url(#shieldGrad)"
            fillOpacity="0.15"
            stroke="url(#shieldGrad)"
            strokeWidth="2.5"
            strokeLinejoin="round"
          />
          <path
            d="M16 25l6 6 12-12"
            stroke="#22D3EE"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle
            cx="24"
            cy="24"
            r="17"
            stroke="#6366F1"
            strokeWidth="1.5"
            strokeDasharray="3 3"
            opacity="0.6"
          />
        </g>
        {/* Brand Text */}
        <text
          x="64"
          y="35"
          fontFamily="Inter, -apple-system, sans-serif"
          fontWeight="800"
          fontSize="24"
          fill="url(#textGrad)"
          letterSpacing="1.5"
        >
          PARAKH
        </text>
        <rect
          x="180"
          y="21"
          width="46"
          height="18"
          rx="4"
          fill="#1E293B"
          stroke="#334155"
          strokeWidth="1"
        />
        <text
          x="203"
          y="33"
          fontFamily="Inter, -apple-system, sans-serif"
          fontWeight="600"
          fontSize="9"
          fill="#22D3EE"
          textAnchor="middle"
          letterSpacing="0.5"
        >
          SECURE
        </text>
        <text
          x="64"
          y="49"
          fontFamily="Inter, -apple-system, sans-serif"
          fontWeight="500"
          fontSize="10.5"
          fill="#94A3B8"
          letterSpacing="0.4"
        >
          OPPORTUNITY VERIFICATION
        </text>
      </svg>
      {showSubtitle && (
        <span className="hidden sm:inline font-mono text-[11px] text-primary tracking-wider uppercase pl-2 border-l border-surface-variant">
          {subtitle}
        </span>
      )}
    </div>
  );
}
