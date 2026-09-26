import React from 'react';

export function Recommendation({
  icon = 'shield',
  title,
  description,
  badgeText = 'Action Required',
  badgeIcon = 'priority_high',
  accent = 'primary' // 'error', 'primary', 'secondary', 'tertiary'
}) {
  const accentConfigs = {
    error: {
      iconBg: 'bg-error-container/30 text-error',
      badgeColor: 'text-error'
    },
    primary: {
      iconBg: 'bg-primary-container/20 text-primary',
      badgeColor: 'text-primary'
    },
    secondary: {
      iconBg: 'bg-secondary-container/40 text-secondary',
      badgeColor: 'text-secondary'
    },
    tertiary: {
      iconBg: 'bg-tertiary-container/30 text-tertiary',
      badgeColor: 'text-tertiary'
    }
  }[accent] || {
    iconBg: 'bg-primary-container/20 text-primary',
    badgeColor: 'text-primary'
  };

  return (
    <div className="p-4 sm:p-5 rounded-xl bg-surface-container hover:bg-surface-container-high transition-all border border-surface-variant/30 flex flex-col justify-between shadow-sm">
      <div className="space-y-2.5">
        <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${accentConfigs.iconBg}`}>
          <span className="material-symbols-outlined text-[20px]">{icon}</span>
        </div>
        <h3 className="font-headline-sm text-sm sm:text-base text-on-surface font-semibold pt-1">
          {title}
        </h3>
        <p className="font-body-sm text-xs sm:text-sm text-on-surface-variant leading-relaxed">
          {description}
        </p>
      </div>
      {badgeText && (
        <div className={`pt-3 mt-3 border-t border-surface-variant/20 font-mono text-[11px] font-semibold flex items-center gap-1.5 ${accentConfigs.badgeColor}`}>
          <span className="material-symbols-outlined text-[14px]">{badgeIcon}</span>
          <span>{badgeText}</span>
        </div>
      )}
    </div>
  );
}

/**
 * Numbered Actionable Recommendations list component
 * Renders structured items or plain text strings as clean, ordered security advisories.
 */
export function Recommendations({ items = [] }) {
  const defaultItems = [
    'Do not pay any registration fee, security deposit, or hardware charge under any circumstance.',
    'Verify the recruiter independently by contacting the company via verified corporate phone or email.',
    'Avoid clicking on suspicious external links, portals, or anonymous forms.',
    'Contact the company through its official verified website to cross-check the vacancy.'
  ];

  const list = items && items.length > 0 ? items : defaultItems;

  return (
    <div className="space-y-3">
      {list.map((rec, index) => {
        const text = typeof rec === 'string' ? rec : rec.description || rec.title;
        const title = typeof rec === 'object' && rec.title && rec.description ? rec.title : null;

        return (
          <div
            key={index}
            className="p-4 rounded-xl bg-surface-container border border-surface-variant/25 flex items-start gap-3.5 hover:bg-surface-container-high transition-colors"
          >
            <div className="w-6 h-6 rounded-full bg-primary/10 border border-primary/30 text-primary flex items-center justify-center font-mono text-xs font-bold shrink-0 mt-0.5">
              {index + 1}
            </div>
            <div className="space-y-0.5 min-w-0">
              {title && (
                <div className="font-headline-sm text-sm text-on-surface font-semibold">
                  {title}
                </div>
              )}
              <p className="font-body-sm text-xs sm:text-sm text-on-surface-variant leading-relaxed">
                {text}
              </p>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default Recommendation;
