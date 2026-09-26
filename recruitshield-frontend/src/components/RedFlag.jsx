import React from 'react';

export function RedFlag({
  icon = 'gpp_bad',
  title,
  severity = 'High Risk',
  description,
  highlightSnippet
}) {
  const displayTitle = typeof title === 'string' ? title : title?.title || '';
  const displayDesc = description || (typeof title === 'object' ? title?.description : '');
  const displaySeverity = severity || (typeof title === 'object' ? title?.severity : 'High Risk');
  const displaySnippet = highlightSnippet || (typeof title === 'object' ? title?.highlightSnippet : null);
  const displayIcon = icon || (typeof title === 'object' ? title?.icon : 'gpp_bad');

  return (
    <div className="p-3.5 sm:p-4 rounded-xl bg-surface-container hover:bg-surface-container-high transition-colors border border-surface-variant/20">
      <div className="flex items-start gap-3">
        <span className="material-symbols-outlined text-error text-[22px] shrink-0 mt-0.5">
          {displayIcon}
        </span>
        <div className="space-y-1 w-full min-w-0">
          <div className="flex flex-wrap items-center justify-between gap-1.5">
            <span className="font-headline-sm text-sm sm:text-base text-on-surface font-semibold">
              {displayTitle}
            </span>
            {displaySeverity && (
              <span className="font-mono text-[10px] text-error uppercase px-1.5 py-0.5 bg-error-container/30 border border-error/20 rounded">
                {displaySeverity}
              </span>
            )}
          </div>
          {displayDesc && (
            <p className="font-body-md text-xs sm:text-sm text-on-surface-variant leading-relaxed">
              {displayDesc}
            </p>
          )}
          {displaySnippet && (
            <div className="pt-1">
              <code className="font-mono text-[11px] text-error px-2 py-0.5 rounded bg-surface-container-lowest border border-error/20 inline-block truncate max-w-full">
                {displaySnippet}
              </code>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function RedFlags({ flags = [] }) {
  if (!flags || flags.length === 0) {
    return (
      <div className="p-4 rounded-xl bg-surface-container/60 border border-surface-variant/20 text-xs text-on-surface-variant text-center">
        No critical red flags detected.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {flags.map((item, idx) => {
        if (typeof item === 'string') {
          return (
            <RedFlag
              key={idx}
              title={item}
              severity="Flagged"
            />
          );
        }
        return (
          <RedFlag
            key={idx}
            icon={item.icon}
            title={item.title}
            severity={item.severity}
            description={item.description}
            highlightSnippet={item.highlightSnippet}
          />
        );
      })}
    </div>
  );
}

export default RedFlag;
