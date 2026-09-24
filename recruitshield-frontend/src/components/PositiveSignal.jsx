import React from 'react';

export function PositiveSignal({ title, description }) {
  const displayTitle = typeof title === 'string' ? title : title?.title || '';
  const displayDesc = description || (typeof title === 'object' ? title?.description : '');

  return (
    <div className="p-4 rounded-xl bg-surface-container border border-surface-variant/20 flex items-start gap-3 hover:bg-surface-container-high transition-colors">
      <span className="material-symbols-outlined text-tertiary text-[20px] shrink-0 mt-0.5">
        check_circle
      </span>
      <div className="space-y-1 min-w-0">
        <span className="font-headline-sm text-sm text-on-surface font-semibold block">
          {displayTitle}
        </span>
        {displayDesc && (
          <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
            {displayDesc}
          </p>
        )}
      </div>
    </div>
  );
}

export function PositiveSignals({ signals = [] }) {
  if (!signals || signals.length === 0) {
    return (
      <div className="p-4 rounded-xl bg-surface-container/60 border border-surface-variant/20 text-xs text-on-surface-variant text-center">
        No positive trust signals confirmed.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {signals.map((item, idx) => {
        if (typeof item === 'string') {
          return <PositiveSignal key={idx} title={item} />;
        }
        return (
          <PositiveSignal
            key={idx}
            title={item.title}
            description={item.description}
          />
        );
      })}
    </div>
  );
}

export default PositiveSignals;
