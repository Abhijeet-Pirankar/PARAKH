import React from 'react';

/**
 * Loading state during offer analysis.
 * Displays "Analyzing offer..." with clean pipeline step indicators and progress.
 */
export default function LoadingAnalysis({ currentStep = 2, message = "Analyzing offer..." }) {
  const steps = [
    { label: 'Content Analysis', icon: 'description' },
    { label: 'Recruiter Check', icon: 'person_search' },
    { label: 'Company Check', icon: 'domain' },
    { label: 'URL Analysis', icon: 'link' },
    { label: 'Risk Assessment', icon: 'shield' }
  ];

  return (
    <div className="rounded-2xl bg-surface-container-low p-8 sm:p-12 border border-surface-variant/40 shadow-xl flex flex-col items-center text-center max-w-xl mx-auto space-y-6">
      {/* Radar/Pill Icon */}
      <div className="relative w-16 h-16 flex items-center justify-center">
        <div className="absolute inset-0 rounded-full bg-primary/10 animate-ping opacity-60" />
        <div className="w-14 h-14 rounded-full bg-surface-container-high border border-primary/40 flex items-center justify-center text-primary shadow-[0_0_24px_rgba(6,182,212,0.25)]">
          <span className="material-symbols-outlined text-[28px] animate-spin">
            sync
          </span>
        </div>
      </div>

      {/* Primary Message */}
      <div className="space-y-1">
        <h3 className="font-headline text-lg sm:text-xl text-on-surface font-bold">
          {message}
        </h3>
        <p className="font-body-sm text-xs sm:text-sm text-on-surface-variant">
          Evaluating recruitment signals, contact channels, and known scam indicators...
        </p>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-surface-container-lowest h-1.5 rounded-full overflow-hidden border border-surface-variant/20 max-w-sm">
        <div className="h-full bg-gradient-to-r from-primary via-primary-container to-secondary w-2/3 animate-pulse rounded-full" />
      </div>

      {/* Pipeline Micro-steps */}
      <div className="grid grid-cols-5 gap-2 w-full pt-2">
        {steps.map((step, idx) => {
          const isDone = idx < currentStep;
          const isCurrent = idx === currentStep;

          return (
            <div key={idx} className="flex flex-col items-center gap-1.5 text-center">
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center text-[16px] transition-colors ${
                  isDone
                    ? 'bg-tertiary/15 text-tertiary border border-tertiary/30'
                    : isCurrent
                    ? 'bg-primary/20 text-primary border border-primary/50 animate-pulse'
                    : 'bg-surface-container text-outline border border-surface-variant/20'
                }`}
              >
                <span className="material-symbols-outlined text-[16px]">
                  {isDone ? 'check' : step.icon}
                </span>
              </div>
              <span
                className={`text-[10px] font-mono leading-tight ${
                  isCurrent
                    ? 'text-primary font-semibold'
                    : isDone
                    ? 'text-on-surface'
                    : 'text-on-surface-variant opacity-60'
                }`}
              >
                {step.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
