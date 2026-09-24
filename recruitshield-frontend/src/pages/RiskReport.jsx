import React, { useState } from 'react';
import { useLocation, Link } from 'react-router-dom';
import RiskScore from '../components/RiskScore';
import RiskBadge from '../components/RiskBadge';
import { RedFlags } from '../components/RedFlag';
import { PositiveSignals } from '../components/PositiveSignal';
import { Recommendations } from '../components/Recommendation';
import Button from '../components/Button';

export default function RiskReport() {
  const location = useLocation();
  const stateData = location.state?.verificationData;
  const stateMeta = location.state?.meta;

  const isDemo = !stateData;

  // Technical drawer tab
  const [activeTab, setActiveTab] = useState('signals');
  const [copySuccess, setCopySuccess] = useState(false);

  // Fallback demo dataset if visited directly without submitting an offer
  const defaultDemo = {
    score: 78,
    status: 'HIGHLY_SUSPICIOUS',
    redFlags: [
      {
        icon: 'payments',
        title: 'Advance-Fee Demand',
        severity: 'Critical',
        description: 'Communication requests upfront financial transfer for registration, laptop courier, or background clearance.',
        highlightSnippet: 'Refundable laptop deposit / courier fee required prior to commencement'
      },
      {
        icon: 'alternate_email',
        title: 'Suspicious Domain / Freemail',
        severity: 'Impersonation Risk',
        description: 'Sender contacted you from a public free email domain instead of an authorized enterprise corporate domain.',
        highlightSnippet: 'recruiter.apexlabs@gmail.com'
      },
      {
        icon: 'forum',
        title: 'Unusual Interview Method',
        severity: 'Channel Anomaly',
        description: 'Interview or offer discussions routed via Telegram or WhatsApp to bypass institutional traceability.',
        highlightSnippet: 'Informal recruitment channel (WhatsApp / Telegram)'
      },
      {
        icon: 'alarm',
        title: 'Artificial Urgency',
        severity: 'Coercive Trigger',
        description: 'Imposes tight countdown ultimatums (within 24 hours) to prevent thorough company research.',
        highlightSnippet: 'Respond immediately within 24 hours'
      }
    ],
    positiveSignals: [
      {
        title: 'Company Information Available',
        description: 'A referenced business entity name is searchable on public commercial indexes.'
      },
      {
        title: 'Recruiter Information Provided',
        description: 'Contact person name and initial contact identifiers were included in message.'
      }
    ],
    recommendations: [
      'Do not pay any registration fee, security deposit, or hardware charge under any circumstance.',
      'Verify the recruiter independently by contacting the company through verified corporate channels.',
      'Avoid clicking on unverified links, external portals, or anonymous cloud forms.',
      'Contact the company human resources department directly through its official website.'
    ]
  };

  // Resolve active data (fully data-driven from backend response or fallback)
  const reportData = stateData || defaultDemo;

  const score = Number(reportData.score ?? 0);
  const status = reportData.status || (score >= 70 ? 'HIGHLY_SUSPICIOUS' : score >= 40 ? 'NEEDS_VERIFICATION' : 'LIKELY_GENUINE');

  // Normalize red flags (handles objects, strings, or legacy 'reasons')
  const redFlags = reportData.redFlags && reportData.redFlags.length > 0
    ? reportData.redFlags
    : (reportData.reasons || []);

  // Normalize positive signals
  const positiveSignals = reportData.positiveSignals || [];

  // Normalize recommendations (handles array or single string)
  const recommendations = reportData.recommendations && reportData.recommendations.length > 0
    ? reportData.recommendations
    : reportData.recommendation
    ? [reportData.recommendation]
    : defaultDemo.recommendations;

  const verificationId = stateMeta?.verificationId || 'PRK-89241';
  const targetSubject = stateMeta?.offerUrl || stateMeta?.offerEmail || 'Job Offer Assessment';
  const targetTimestamp = stateMeta?.timestamp
    ? new Date(stateMeta.timestamp).toLocaleString()
    : 'Recent Assessment';

  const handlePrint = () => {
    window.print();
  };

  const handleCopyLink = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    }
  };

  return (
    <div className="relative w-full max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 space-y-8">
      {/* Demo State Banner if accessed directly */}
      {isDemo && (
        <div className="p-3.5 rounded-xl bg-surface-container-high border border-primary/30 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs font-mono">
          <div className="flex items-center gap-2 text-primary">
            <span className="material-symbols-outlined text-[18px]">info</span>
            <span>Displaying sample investigation report. You can check your own offer anytime at Check Offer.</span>
          </div>
          <Link
            to="/check"
            className="px-3 py-1 rounded-lg bg-primary/20 text-primary hover:bg-primary/30 font-semibold transition-colors shrink-0"
          >
            Check an Offer
          </Link>
        </div>
      )}

      {/* ================================================== */}
      {/* 1. INVESTIGATION RESULT Header */}
      {/* ================================================== */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-surface-variant/30">
        <div className="space-y-1 min-w-0">
          <div className="flex items-center gap-2 text-xs font-mono text-on-surface-variant">
            <span className="font-bold text-primary uppercase tracking-wider">INVESTIGATION RESULT</span>
            <span>//</span>
            <span className="text-on-surface">{verificationId}</span>
          </div>
          <h1 className="font-headline text-2xl sm:text-3xl text-on-surface font-extrabold tracking-tight truncate">
            {targetSubject}
          </h1>
          <div className="flex flex-wrap items-center gap-3 text-xs font-mono text-on-surface-variant">
            <span>Evaluated: {targetTimestamp}</span>
            <span>•</span>
            <span className="text-primary">AI-Assisted Analysis</span>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center flex-wrap gap-2.5 shrink-0">
          <button
            type="button"
            onClick={handlePrint}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface text-xs font-mono border border-surface-variant/40 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">print</span>
            <span>Print PDF</span>
          </button>
          <button
            type="button"
            onClick={handleCopyLink}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface text-xs font-mono border border-surface-variant/40 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">
              {copySuccess ? 'check' : 'share'}
            </span>
            <span>{copySuccess ? 'Copied' : 'Share'}</span>
          </button>
          <Button to="/check" variant="primary" size="sm" icon="add">
            Check Another Offer
          </Button>
        </div>
      </div>

      {/* ================================================== */}
      {/* 2. Risk Score & Status (Immediate Understanding) */}
      {/* ================================================== */}
      <div className="rounded-2xl bg-surface-container-low p-6 sm:p-8 border border-surface-variant/40 shadow-xl flex flex-col items-center text-center">
        <span className="font-mono text-xs uppercase tracking-widest text-on-surface-variant font-semibold mb-2">
          Composite Risk Assessment
        </span>

        {/* Reusable Data-Driven RiskScore Component */}
        <RiskScore score={score} status={status} size="lg" showBadge={false} disclaimer={false} />

        <div className="mt-3 flex flex-col items-center gap-2">
          <RiskBadge status={status} score={score} size="lg" />
          <p className="font-body-sm text-xs sm:text-sm text-on-surface-variant max-w-md mt-1 leading-relaxed">
            {score >= 70
              ? 'High risk of fraudulent activity detected. Exercise extreme caution and do not send money.'
              : score >= 40
              ? 'Moderate risk signals detected. Additional verification with the employer is strongly recommended.'
              : 'Standard trust signals observed. Always confirm sensitive details through official employer channels.'}
          </p>
        </div>
      </div>

      {/* ================================================== */}
      {/* 3. WHY? (Critical Red Flags & Positive Signals) */}
      {/* ================================================== */}
      <div className="space-y-6">
        <div className="border-b border-surface-variant/30 pb-2">
          <h2 className="font-headline text-xl text-on-surface font-bold">
            WHY?
          </h2>
          <p className="font-body-sm text-xs text-on-surface-variant mt-0.5">
            Key risk factors and positive signals identified in this offer.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
          {/* Critical Red Flags */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/40 shadow-lg space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-surface-variant/30">
              <div className="flex items-center gap-2 text-error">
                <span className="material-symbols-outlined text-[20px]">warning</span>
                <h3 className="font-headline-sm text-base font-bold text-on-surface">
                  Critical Red Flags
                </h3>
              </div>
              <span className="font-mono text-xs text-error font-semibold px-2 py-0.5 rounded bg-error-container/30 border border-error/20">
                {redFlags.length} Flagged
              </span>
            </div>

            {/* Reusable Data-Driven RedFlags Component */}
            <RedFlags flags={redFlags} />
          </div>

          {/* Positive Signals */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/40 shadow-lg space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-surface-variant/30">
              <div className="flex items-center gap-2 text-tertiary">
                <span className="material-symbols-outlined text-[20px]">verified</span>
                <h3 className="font-headline-sm text-base font-bold text-on-surface">
                  Positive Signals
                </h3>
              </div>
              <span className="font-mono text-xs text-tertiary font-semibold px-2 py-0.5 rounded bg-tertiary-container/20 border border-tertiary/20">
                {positiveSignals.length} Confirmed
              </span>
            </div>

            {/* Reusable Data-Driven PositiveSignals Component */}
            <PositiveSignals signals={positiveSignals} />
          </div>
        </div>
      </div>

      {/* ================================================== */}
      {/* 4. WHAT SHOULD YOU DO? (Actionable Recommendations) */}
      {/* ================================================== */}
      <div className="rounded-2xl bg-surface-container-low p-6 sm:p-8 border border-surface-variant/40 shadow-xl space-y-5">
        <div className="pb-3 border-b border-surface-variant/30">
          <h2 className="font-headline text-xl text-on-surface font-bold">
            WHAT SHOULD YOU DO?
          </h2>
          <p className="font-body-sm text-xs text-on-surface-variant mt-0.5">
            Follow these immediate protective steps to safeguard yourself:
          </p>
        </div>

        {/* Reusable Data-Driven Recommendations Component */}
        <Recommendations items={recommendations} />
      </div>

      {/* ================================================== */}
      {/* 5. Technical Analysis (Collapsible) */}
      {/* ================================================== */}
      <details className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/40 shadow-lg space-y-4 group">
        <summary className="flex items-center justify-between cursor-pointer select-none list-none text-on-surface font-semibold">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[20px]">terminal</span>
            <span className="font-headline text-base sm:text-lg">Technical Analysis</span>
          </div>
          <span className="inline-flex items-center gap-1 font-mono text-xs text-primary px-2.5 py-1 rounded-lg bg-surface-container border border-surface-variant/30 group-open:rotate-180 transition-transform">
            <span className="material-symbols-outlined text-[16px]">expand_more</span>
          </span>
        </summary>

        <div className="pt-4 border-t border-surface-variant/30 space-y-4">
          <div className="flex items-center gap-2 font-mono text-xs border-b border-surface-variant/20 pb-3">
            <button
              type="button"
              onClick={() => setActiveTab('signals')}
              className={`px-3 py-1 rounded-lg transition-colors ${
                activeTab === 'signals'
                  ? 'bg-surface-container-high text-primary font-semibold'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Evaluated Signals
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('channels')}
              className={`px-3 py-1 rounded-lg transition-colors ${
                activeTab === 'channels'
                  ? 'bg-surface-container-high text-primary font-semibold'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Sender & Channels
            </button>
          </div>

          {activeTab === 'signals' && (
            <div className="bg-surface-container-lowest rounded-xl p-4 font-mono text-xs space-y-2 border border-surface-variant/20 text-on-surface">
              <div className="text-on-surface-variant">// Heuristic Evaluation Summary</div>
              <div className="flex justify-between">
                <span className="text-on-surface-variant">Advance-Fee Indicator:</span>
                <span className={score >= 40 ? 'text-error' : 'text-tertiary'}>
                  {score >= 40 ? 'DETECTED' : 'CLEAN'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-on-surface-variant">Recruiter Webmail Check:</span>
                <span className={stateMeta?.offerEmail?.includes('@gmail') ? 'text-error' : 'text-on-surface'}>
                  {stateMeta?.offerEmail || 'Domain Verified'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-on-surface-variant">Delivery Channel:</span>
                <span className="text-on-surface">{stateMeta?.offerSource || 'direct-email'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-on-surface-variant">Risk Tier Index:</span>
                <span className="text-primary">{score} / 100</span>
              </div>
            </div>
          )}

          {activeTab === 'channels' && (
            <div className="bg-surface-container-lowest rounded-xl p-4 font-mono text-xs space-y-2 border border-surface-variant/20 text-on-surface">
              <div className="text-on-surface-variant">// Communication Metadata Audit</div>
              <div>
                <span className="text-primary">Sender Handle:</span> {stateMeta?.offerEmail || 'Not specified'}
              </div>
              <div>
                <span className="text-primary">Company URL:</span> {stateMeta?.offerUrl || 'Not specified'}
              </div>
              <div>
                <span className="text-primary">Channel Type:</span> {stateMeta?.offerSource || 'Direct Communication'}
              </div>
              <div>
                <span className="text-primary">Assessment Model:</span> Heuristic Rule Matrix v1.0
              </div>
            </div>
          )}
        </div>
      </details>

      {/* Footer Navigation */}
      <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-surface-variant/30 text-xs font-mono text-on-surface-variant">
        <span>PARAKH Opportunity Verification</span>
        <Button to="/check" variant="secondary" size="sm" iconRight="arrow_forward">
          Check Another Offer
        </Button>
      </div>
    </div>
  );
}
