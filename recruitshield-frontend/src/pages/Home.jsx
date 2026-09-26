import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';

export default function Home() {
  const videoRef = React.useRef(null);

  React.useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const handleMotion = () => {
      if (videoRef.current) {
        if (mediaQuery.matches) {
          videoRef.current.pause();
        } else {
          videoRef.current.play().catch(() => {});
        }
      }
    };
    handleMotion();
    mediaQuery.addEventListener?.('change', handleMotion);
    return () => mediaQuery.removeEventListener?.('change', handleMotion);
  }, []);

  const pipelineSteps = [
    {
      step: '01',
      title: 'Offer',
      description: 'Candidate submits job or internship offer communication.',
      icon: 'description'
    },
    {
      step: '02',
      title: 'Content Analysis',
      description: 'Scans for advance-fee traps, payment demands, and coercive pressure.',
      icon: 'manage_search'
    },
    {
      step: '03',
      title: 'Recruiter Check',
      description: 'Detects free webmail handles (@gmail, @yahoo) and sender anomalies.',
      icon: 'person_search'
    },
    {
      step: '04',
      title: 'Company Check',
      description: 'Evaluates employer domain authenticity and brand consistency.',
      icon: 'domain'
    },
    {
      step: '05',
      title: 'URL Analysis',
      description: 'Screens links for typosquatting, shorteners, and suspicious domains.',
      icon: 'link'
    },
    {
      step: '06',
      title: 'Risk Assessment',
      description: 'Synthesizes findings into an explainable score with clear next steps.',
      icon: 'shield'
    }
  ];

  return (
    <div className="relative w-full overflow-hidden">
      {/* ================================================== */}
      {/* 1. Hero Section */}
      {/* ================================================== */}
      <section className="hero w-full">
        {/* Motion Background Video */}
        <video
          ref={videoRef}
          className="hero-video"
          autoPlay
          loop
          muted
          playsInline
          preload="auto"
          aria-hidden="true"
        >
          <source src="/hero-bg.mp4" type="video/mp4" />
        </video>

        {/* Dark Navy Overlay */}
        <div className="hero-overlay" aria-hidden="true" />

        {/* Subtle Top Ambient Accent */}
        <div className="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 w-[700px] h-[350px] bg-[radial-gradient(ellipse_at_center,rgba(6,182,212,0.12)_0%,transparent_70%)] blur-3xl z-[1]" />

        {/* Hero Content */}
        <div className="hero-content relative z-[2] w-full max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-12 sm:pt-20 pb-16 text-center">
          {/* Badge Pill */}
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-surface-container-high border border-surface-variant/40 mb-6">
            <span className="relative flex h-2 w-2">
              <span className="relative inline-flex rounded-full h-2 w-2 bg-primary" />
            </span>
            <span className="font-mono text-xs text-primary font-medium tracking-wider uppercase">
              AI-Assisted Risk Assessment
            </span>
          </div>

          {/* Brand & Headline */}
          <div className="mb-4">
            <span className="font-mono text-xs font-bold text-primary uppercase tracking-widest block mb-2">
              PARAKH
            </span>
            <h1 className="font-headline text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-on-surface leading-tight">
              Verify Before You{' '}
              <span className="bg-gradient-to-r from-primary via-primary-container to-secondary bg-clip-text text-transparent">
                Trust.
              </span>
            </h1>
          </div>

          {/* Value Proposition */}
          <p className="font-body text-base sm:text-lg text-on-surface-variant max-w-2xl mx-auto mb-8 leading-relaxed">
            Check a job or internship offer for suspicious signals. Detect advance-fee demands, recruiter impersonation, and deceptive domains before you commit.
          </p>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3.5 mb-12">
            <Button
              to="/check"
              variant="primary"
              size="lg"
              iconRight="arrow_forward"
              className="w-full sm:w-auto"
            >
              Check an Offer
            </Button>
            <Button
              href="#how-it-works"
              variant="secondary"
              size="lg"
              className="w-full sm:w-auto"
            >
              How It Works
            </Button>
          </div>

          {/* Quick Trust Attributes */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-6 border-t border-surface-variant/20 max-w-3xl mx-auto text-left">
            <div className="flex items-center gap-2 text-on-surface-variant">
              <span className="material-symbols-outlined text-primary text-[18px]">verified</span>
              <span className="text-xs font-medium text-on-surface">AI-Assisted Analysis</span>
            </div>
            <div className="flex items-center gap-2 text-on-surface-variant">
              <span className="material-symbols-outlined text-primary text-[18px]">domain_verification</span>
              <span className="text-xs font-medium text-on-surface">Recruiter & Domain Checks</span>
            </div>
            <div className="flex items-center gap-2 text-on-surface-variant">
              <span className="material-symbols-outlined text-primary text-[18px]">shield</span>
              <span className="text-xs font-medium text-on-surface">Explainable Scoring</span>
            </div>
            <div className="flex items-center gap-2 text-on-surface-variant">
              <span className="material-symbols-outlined text-tertiary text-[18px]">lock</span>
              <span className="text-xs font-medium text-on-surface">Privacy & Security</span>
            </div>
          </div>
        </div>
      </section>

      {/* ================================================== */}
      {/* 2. PARAKH Verification Pipeline */}
      {/* ================================================== */}
      <section id="pipeline" className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center max-w-2xl mx-auto mb-10">
          <div className="inline-flex items-center gap-1.5 text-primary font-mono text-xs uppercase tracking-wider mb-2">
            <span className="material-symbols-outlined text-[16px]">account_tree</span>
            Verification Flow
          </div>
          <h2 className="font-headline text-2xl sm:text-3xl text-on-surface font-bold tracking-tight">
            PARAKH Verification Pipeline
          </h2>
          <p className="font-body-sm text-sm text-on-surface-variant mt-2">
            A systematic, multi-stage assessment designed to inspect key opportunity vectors.
          </p>
        </div>

        {/* Flow Container */}
        <div className="rounded-2xl bg-surface-container-low p-6 sm:p-8 border border-surface-variant/40 shadow-lg">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4 relative">
            {pipelineSteps.map((s, idx) => (
              <div
                key={idx}
                className="relative rounded-xl bg-surface-container p-4 border border-surface-variant/30 flex flex-col justify-between hover:bg-surface-container-high transition-colors"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="w-7 h-7 rounded-lg bg-surface-container-lowest flex items-center justify-center text-primary border border-surface-variant/30">
                      <span className="material-symbols-outlined text-[16px]">{s.icon}</span>
                    </span>
                    <span className="font-mono text-[11px] text-on-surface-variant font-semibold">
                      {s.step}
                    </span>
                  </div>
                  <h3 className="font-headline-sm text-sm text-on-surface font-semibold mb-1">
                    {s.title}
                  </h3>
                  <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                    {s.description}
                  </p>
                </div>

                {/* Desktop Arrow Connector */}
                {idx < pipelineSteps.length - 1 && (
                  <div className="hidden lg:block absolute -right-3 top-1/2 -translate-y-1/2 z-10 text-on-surface-variant/60 pointer-events-none">
                    <span className="material-symbols-outlined text-[18px]">chevron_right</span>
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="mt-6 pt-4 border-t border-surface-variant/20 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-on-surface-variant font-mono">
            <span>Result: Comprehensive Investigation Report with actionable guidance.</span>
            <Link
              to="/check"
              className="inline-flex items-center gap-1 text-primary hover:underline font-semibold"
            >
              <span>Test an offer through the pipeline</span>
              <span className="material-symbols-outlined text-[15px]">arrow_forward</span>
            </Link>
          </div>
        </div>
      </section>

      {/* ================================================== */}
      {/* 3. Core Verification Features */}
      {/* ================================================== */}
      <section id="features" className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center max-w-2xl mx-auto mb-10">
          <div className="inline-flex items-center gap-1.5 text-primary font-mono text-xs uppercase tracking-wider mb-2">
            <span className="material-symbols-outlined text-[16px]">shield</span>
            Core Capabilities
          </div>
          <h2 className="font-headline text-2xl sm:text-3xl text-on-surface font-bold tracking-tight">
            Core Verification Features
          </h2>
          <p className="font-body-sm text-sm text-on-surface-variant mt-2">
            Key threat signals and authenticity factors examined during analysis.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Card 1 */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col justify-between hover:bg-surface-container transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-primary-container/15 text-primary flex items-center justify-center mb-4">
                <span className="material-symbols-outlined text-[22px]">payments</span>
              </div>
              <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
                Content & Fee Analysis
              </h3>
              <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                Flags requests for registration fees, equipment deposits, and artificial countdown urgency.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-surface-variant/20 font-mono text-[11px] text-error font-medium flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px]">flag</span>
              <span>Advance-Fee Detection</span>
            </div>
          </div>

          {/* Card 2 */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col justify-between hover:bg-surface-container transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-secondary-container/30 text-secondary flex items-center justify-center mb-4">
                <span className="material-symbols-outlined text-[22px]">alternate_email</span>
              </div>
              <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
                Recruiter Check
              </h3>
              <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                Detects recruiters using free webmail addresses (@gmail, @yahoo) claiming corporate representation.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-surface-variant/20 font-mono text-[11px] text-secondary font-medium flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px]">badge</span>
              <span>Impersonation Check</span>
            </div>
          </div>

          {/* Card 3 */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col justify-between hover:bg-surface-container transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-tertiary/15 text-tertiary flex items-center justify-center mb-4">
                <span className="material-symbols-outlined text-[22px]">domain</span>
              </div>
              <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
                Company Validation
              </h3>
              <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                Checks company website presence, domain alignment, and flags inconsistencies.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-surface-variant/20 font-mono text-[11px] text-tertiary font-medium flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px]">verified</span>
              <span>Corporate Consistency</span>
            </div>
          </div>

          {/* Card 4 */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col justify-between hover:bg-surface-container transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-primary-container/15 text-primary flex items-center justify-center mb-4">
                <span className="material-symbols-outlined text-[22px]">link</span>
              </div>
              <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
                URL & Domain Analysis
              </h3>
              <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                Identifies typosquatting domains, link shorteners, and suspicious generic TLDs (.xyz, .top).
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-surface-variant/20 font-mono text-[11px] text-primary font-medium flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px]">travel_explore</span>
              <span>Phishing Vector Screening</span>
            </div>
          </div>
        </div>
      </section>

      {/* ================================================== */}
      {/* 4. How PARAKH Works */}
      {/* ================================================== */}
      <section id="how-it-works" className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center max-w-2xl mx-auto mb-10">
          <div className="inline-flex items-center gap-1.5 text-secondary font-mono text-xs uppercase tracking-wider mb-2">
            <span className="material-symbols-outlined text-[16px]">help_outline</span>
            Simple Protocol
          </div>
          <h2 className="font-headline text-2xl sm:text-3xl text-on-surface font-bold tracking-tight">
            How PARAKH Works
          </h2>
          <p className="font-body-sm text-sm text-on-surface-variant mt-2">
            No registration needed. Fast and objective opportunity verification.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col">
            <div className="w-10 h-10 rounded-xl bg-surface-container-high font-mono font-bold text-primary flex items-center justify-center mb-4">
              01
            </div>
            <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
              Paste Offer Details
            </h3>
            <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
              Provide the offer letter text, email message, company website, or recruiter contact details.
            </p>
          </div>

          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col">
            <div className="w-10 h-10 rounded-xl bg-surface-container-high font-mono font-bold text-primary flex items-center justify-center mb-4">
              02
            </div>
            <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
              Run Risk Assessment
            </h3>
            <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
              PARAKH evaluates the input against common recruitment scam patterns and domain indicators.
            </p>
          </div>

          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/30 flex flex-col">
            <div className="w-10 h-10 rounded-xl bg-surface-container-high font-mono font-bold text-tertiary flex items-center justify-center mb-4">
              03
            </div>
            <h3 className="font-headline-sm text-base text-on-surface font-semibold mb-2">
              Get Actionable Report
            </h3>
            <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
              Review your overall Risk Score, confirmed red flags, positive signals, and protective next steps.
            </p>
          </div>
        </div>
      </section>

      {/* ================================================== */}
      {/* 5. Final Call to Action */}
      {/* ================================================== */}
      <section className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 mb-8">
        <div className="rounded-2xl bg-surface-container p-8 sm:p-12 text-center border border-surface-variant/40 shadow-xl max-w-4xl mx-auto">
          <span className="font-mono text-xs uppercase tracking-widest text-primary font-semibold block mb-2">
            Verify Before You Trust
          </span>
          <h2 className="font-headline text-2xl sm:text-3xl text-on-surface font-bold tracking-tight mb-3">
            Have a Job or Internship Offer?
          </h2>
          <p className="font-body text-sm sm:text-base text-on-surface-variant max-w-xl mx-auto mb-8 leading-relaxed">
            Take two minutes to check for red flags before sharing personal information or signing documents.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3.5">
            <Button
              to="/check"
              variant="primary"
              size="lg"
              iconRight="arrow_forward"
              className="w-full sm:w-auto"
            >
              Check an Offer
            </Button>
            <Button
              to="/report"
              variant="secondary"
              size="lg"
              className="w-full sm:w-auto"
            >
              View Sample Report
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
