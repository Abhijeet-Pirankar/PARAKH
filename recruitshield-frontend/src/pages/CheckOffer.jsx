import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { analyzeOffer } from '../services/api';
import LoadingAnalysis from '../components/LoadingAnalysis';
import Button from '../components/Button';

export default function CheckOffer() {
  const location = useLocation();
  const navigate = useNavigate();

  const [offerText, setOfferText] = useState(location.state?.initialOfferText || '');
  const [offerUrl, setOfferUrl] = useState('');
  const [offerEmail, setOfferEmail] = useState('');
  const [offerSource, setOfferSource] = useState('direct-email');

  // UI States: 'idle' | 'loading' | 'error' | 'invalid_input' | 'success'
  const [uiState, setUiState] = useState('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const [sampleMenuOpen, setSampleMenuOpen] = useState(false);

  // Pre-configured testing samples
  const samples = {
    scamDeposit: {
      title: 'Advance-Fee Internship Scam',
      summary: 'Mandatory laptop deposit via UPI / personal email',
      text: `Congratulations! You have been selected for the Software Engineer Intern role at Apex Global Labs.
Stipend: ₹45,000/month.
Starting Date: Immediate joining without technical interview.

To secure your position and expedite your official laptop delivery, you are required to transfer a refundable security deposit of ₹4,500 via UPI to apex.onboarding@upi within 24 hours. This fee is fully reimbursed on your first monthly stipend.`,
      url: 'https://apex-globallabs-portal.xyz',
      email: 'recruiter.apexlabs@gmail.com',
      source: 'whatsapp'
    },
    scamTelegram: {
      title: 'Remote Data Analyst Scam',
      summary: 'High pay, Telegram interview, wire transfer',
      text: `Dear Applicant,
Our recruitment panel reviewed your profile and shortlisted you for Remote Data Analyst ($55/hour). 
No technical interview is needed. You have been directly selected.
Please contact our hiring coordinator on Telegram (@ApexRecruitOffice) within 12 hours to confirm your bank account and receive equipment purchase funds.`,
      url: 'https://careers-apexdata.xyz',
      email: 'hr-department@gmail.com',
      source: 'telegram'
    },
    legitimateOffer: {
      title: 'Standard Legitimate Offer',
      summary: 'Corporate domain, standard onboarding, zero fees',
      text: `Dear Candidate,
We are pleased to offer you the position of Junior Software Engineer at Acme Corporation.
Starting Date: August 1st, 2025.
Annual Base Salary: $85,000 paid bi-weekly.

Please review your formal offer contract on our official corporate workday portal at https://acmecorp.com/careers. No payment, deposit, or hardware fee is ever required.`,
      url: 'https://acmecorp.com',
      email: 'talent@acmecorp.com',
      source: 'direct-email'
    }
  };

  const handleLoadSample = (key) => {
    const s = samples[key];
    if (s) {
      setOfferText(s.text);
      setOfferUrl(s.url);
      setOfferEmail(s.email);
      setOfferSource(s.source);
      setUiState('idle');
      setErrorMessage('');
      setSampleMenuOpen(false);
    }
  };

  const handleClear = () => {
    setOfferText('');
    setOfferUrl('');
    setOfferEmail('');
    setOfferSource('direct-email');
    setUiState('idle');
    setErrorMessage('');
  };

  const handlePaste = async () => {
    try {
      if (navigator.clipboard) {
        const text = await navigator.clipboard.readText();
        if (text) {
          setOfferText(text);
          setUiState('idle');
          setErrorMessage('');
        }
      }
    } catch {
      // Ignore clipboard permission denial
    }
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();

    // Validation: Require offer message
    if (!offerText || !offerText.trim()) {
      setUiState('invalid_input');
      setErrorMessage('Please enter an offer message.');
      return;
    }

    // Optional email format validation
    if (offerEmail && offerEmail.trim()) {
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailPattern.test(offerEmail.trim())) {
        setUiState('invalid_input');
        setErrorMessage('Please enter a valid recruiter email address (e.g. recruiter@company.com).');
        return;
      }
    }

    setUiState('loading');
    setErrorMessage('');

    try {
      const payload = {
        offerText: offerText.trim(),
        companyName: '',
        companyWebsite: offerUrl.trim(),
        recruiterEmail: offerEmail.trim(),
        receivedVia: offerSource
      };

      const result = await analyzeOffer(payload);

      setUiState('success');

      // Brief delay to allow success transition then navigate
      setTimeout(() => {
        navigate('/report', {
          state: {
            verificationData: result,
            meta: {
              offerText,
              offerUrl,
              offerEmail,
              offerSource,
              timestamp: new Date().toISOString(),
              verificationId: `PRK-${Math.floor(10000 + Math.random() * 90000)}`
            }
          }
        });
      }, 500);
    } catch (err) {
      setUiState('error');
      if (err.code === 'BACKEND_UNAVAILABLE') {
        setErrorMessage('Unable to connect to PARAKH backend service. Please ensure the Spring Boot server is running on http://localhost:8080.');
      } else if (err.status === 400) {
        setErrorMessage(err.message || 'Validation error: please check your offer inputs.');
      } else {
        setErrorMessage(err.message || 'Unable to analyze this offer. Please check your connection and try again.');
      }
    }
  };

  return (
    <div className="relative w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
      {/* Page Title & Context */}
      <div className="max-w-3xl mb-8">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-high border border-surface-variant/40 mb-3 text-xs font-mono text-primary">
          <span className="material-symbols-outlined text-[15px]">verified</span>
          <span>AI-Assisted Risk Assessment</span>
        </div>
        <h1 className="font-headline text-2xl sm:text-4xl text-on-surface font-extrabold tracking-tight mb-2">
          Check a Job or Internship Offer
        </h1>
        <p className="font-body text-sm sm:text-base text-on-surface-variant leading-relaxed">
          Paste the offer text or email message below. PARAKH checks for upfront fee traps, recruiter impersonation, unverified contact channels, and suspicious links.
        </p>
      </div>

      {/* Main Grid: Form Console & Information Column */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Analysis Form (7 or 8 cols) */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          <div className="rounded-2xl bg-surface-container-low p-6 sm:p-8 border border-surface-variant/40 shadow-xl relative">
            {/* Top Toolbar: Label & Sample Loader */}
            <div className="flex flex-wrap items-center justify-between gap-3 pb-4 mb-4 border-b border-surface-variant/30">
              <span className="font-mono text-xs font-bold uppercase tracking-wider text-on-surface flex items-center gap-1.5">
                <span className="material-symbols-outlined text-primary text-[18px]">edit_note</span>
                Offer Details
              </span>

              {/* Sample Loader Dropdown */}
              <div className="relative inline-block text-left">
                <button
                  type="button"
                  onClick={() => setSampleMenuOpen(!sampleMenuOpen)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface text-xs font-mono border border-surface-variant/40 transition-colors"
                >
                  <span className="material-symbols-outlined text-primary text-[16px]">file_open</span>
                  <span>Load Sample Offer</span>
                  <span className="material-symbols-outlined text-[16px]">expand_more</span>
                </button>

                {sampleMenuOpen && (
                  <div className="absolute right-0 mt-2 w-72 rounded-xl bg-surface-container-high border border-surface-variant/50 shadow-2xl z-30 p-2 space-y-1">
                    <button
                      type="button"
                      onClick={() => handleLoadSample('scamDeposit')}
                      className="w-full text-left p-2 rounded-lg hover:bg-surface-variant transition-colors"
                    >
                      <div className="font-mono text-xs text-error font-semibold">
                        {samples.scamDeposit.title}
                      </div>
                      <div className="text-[11px] text-on-surface-variant truncate">
                        {samples.scamDeposit.summary}
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleLoadSample('scamTelegram')}
                      className="w-full text-left p-2 rounded-lg hover:bg-surface-variant transition-colors"
                    >
                      <div className="font-mono text-xs text-error font-semibold">
                        {samples.scamTelegram.title}
                      </div>
                      <div className="text-[11px] text-on-surface-variant truncate">
                        {samples.scamTelegram.summary}
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleLoadSample('legitimateOffer')}
                      className="w-full text-left p-2 rounded-lg hover:bg-surface-variant transition-colors"
                    >
                      <div className="font-mono text-xs text-tertiary font-semibold">
                        {samples.legitimateOffer.title}
                      </div>
                      <div className="text-[11px] text-on-surface-variant truncate">
                        {samples.legitimateOffer.summary}
                      </div>
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* UI State: LOADING */}
            {uiState === 'loading' ? (
              <div className="py-6">
                <LoadingAnalysis message="Analyzing offer..." />
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* UI State: INVALID INPUT */}
                {uiState === 'invalid_input' && (
                  <div className="p-4 rounded-xl bg-error-container/20 border border-error/30 flex items-center justify-between gap-3 text-error text-sm">
                    <div className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-[20px]">error</span>
                      <span>{errorMessage || 'Please enter an offer message.'}</span>
                    </div>
                  </div>
                )}

                {/* UI State: ERROR */}
                {uiState === 'error' && (
                  <div className="p-4 rounded-xl bg-error-container/20 border border-error/30 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-error text-sm">
                    <div className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-[20px]">warning</span>
                      <span>{errorMessage || 'Unable to analyze this offer.'}</span>
                    </div>
                    <button
                      type="button"
                      onClick={handleSubmit}
                      className="px-3 py-1 rounded-lg bg-error-container text-on-error-container text-xs font-semibold hover:brightness-110 shrink-0"
                    >
                      Try Again
                    </button>
                  </div>
                )}

                {/* UI State: SUCCESS */}
                {uiState === 'success' && (
                  <div className="p-4 rounded-xl bg-tertiary-container/20 border border-tertiary/30 flex items-center gap-2 text-tertiary text-sm">
                    <span className="material-symbols-outlined text-[20px]">check_circle</span>
                    <span>Analysis Complete. Loading investigation report...</span>
                  </div>
                )}

                {/* Offer Textarea (Primary) */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <label htmlFor="offer-text" className="font-mono text-xs text-on-surface font-semibold flex items-center gap-1">
                      <span>Offer Message / Letter Text</span>
                      <span className="text-primary">*</span>
                    </label>
                    <div className="flex items-center gap-3">
                      <button
                        type="button"
                        onClick={handlePaste}
                        className="font-mono text-xs text-primary hover:text-primary-fixed flex items-center gap-1 transition-colors"
                      >
                        <span className="material-symbols-outlined text-[15px]">content_paste</span>
                        <span>Paste</span>
                      </button>
                      <span className="text-surface-variant">|</span>
                      <button
                        type="button"
                        onClick={handleClear}
                        className="font-mono text-xs text-on-surface-variant hover:text-error flex items-center gap-1 transition-colors"
                      >
                        <span className="material-symbols-outlined text-[15px]">close</span>
                        <span>Clear</span>
                      </button>
                    </div>
                  </div>

                  <textarea
                    id="offer-text"
                    value={offerText}
                    onChange={(e) => {
                      setOfferText(e.target.value);
                      if (uiState === 'invalid_input') setUiState('idle');
                    }}
                    rows={7}
                    className={`w-full rounded-xl bg-surface-container-lowest text-on-surface font-mono text-xs sm:text-sm p-4 border transition-all resize-y placeholder:text-outline leading-relaxed focus:outline-none focus:ring-1 focus:ring-primary ${
                      uiState === 'invalid_input' ? 'border-error' : 'border-surface-variant/30'
                    }`}
                    placeholder="Paste the full job offer letter, email body, or recruitment chat conversation here..."
                  />

                  {/* Character/Word Counter or Empty Guidance */}
                  <div className="flex items-center justify-between text-on-surface-variant font-mono text-xs px-1">
                    {offerText.trim().length > 0 ? (
                      <span>
                        {offerText.trim().split(/\s+/).length} words • {offerText.length} characters
                      </span>
                    ) : (
                      <span className="text-on-surface-variant/80">
                        Paste an offer to begin analysis.
                      </span>
                    )}
                    <span className="text-primary flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-primary" />
                      <span>{offerText.trim().length > 0 ? 'Ready' : 'Waiting for text'}</span>
                    </span>
                  </div>
                </div>

                {/* Secondary Context Fields */}
                <div className="pt-4 border-t border-surface-variant/20 space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* Company Website */}
                    <div className="space-y-1.5">
                      <label htmlFor="company-url" className="font-mono text-xs text-on-surface flex items-center gap-1">
                        <span className="material-symbols-outlined text-[15px] text-primary">public</span>
                        <span>Company Website</span>
                      </label>
                      <input
                        id="company-url"
                        type="url"
                        value={offerUrl}
                        onChange={(e) => setOfferUrl(e.target.value)}
                        placeholder="https://company.com"
                        className="w-full bg-surface-container-lowest rounded-lg font-mono text-xs text-on-surface px-3 py-2.5 border border-surface-variant/30 focus:outline-none focus:border-primary placeholder:text-outline"
                      />
                    </div>

                    {/* Recruiter Email */}
                    <div className="space-y-1.5">
                      <label htmlFor="recruiter-email" className="font-mono text-xs text-on-surface flex items-center gap-1">
                        <span className="material-symbols-outlined text-[15px] text-secondary">alternate_email</span>
                        <span>Recruiter Email</span>
                      </label>
                      <input
                        id="recruiter-email"
                        type="email"
                        value={offerEmail}
                        onChange={(e) => setOfferEmail(e.target.value)}
                        placeholder="recruiter@company.com"
                        className="w-full bg-surface-container-lowest rounded-lg font-mono text-xs text-on-surface px-3 py-2.5 border border-surface-variant/30 focus:outline-none focus:border-primary placeholder:text-outline"
                      />
                    </div>

                    {/* Received via */}
                    <div className="space-y-1.5">
                      <label htmlFor="received-via" className="font-mono text-xs text-on-surface flex items-center gap-1">
                        <span className="material-symbols-outlined text-[15px] text-tertiary">chat</span>
                        <span>Received Via</span>
                      </label>
                      <div className="relative">
                        <select
                          id="received-via"
                          value={offerSource}
                          onChange={(e) => setOfferSource(e.target.value)}
                          className="w-full bg-surface-container-lowest rounded-lg font-body text-xs text-on-surface px-3 py-2.5 border border-surface-variant/30 focus:outline-none focus:border-primary appearance-none cursor-pointer"
                        >
                          <option value="direct-email">Corporate Email</option>
                          <option value="linkedin">LinkedIn Message</option>
                          <option value="whatsapp">WhatsApp</option>
                          <option value="telegram">Telegram</option>
                          <option value="college">Campus Placement</option>
                          <option value="indeed">Job Portal (Indeed / Naukri)</option>
                        </select>
                        <span className="material-symbols-outlined absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant text-[18px]">
                          expand_more
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Primary Action Button */}
                <div className="pt-4 border-t border-surface-variant/20 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
                  <div className="text-xs text-on-surface-variant font-mono flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px] text-primary">security</span>
                    <span>Evaluates offer content, recruiter, and domain signals.</span>
                  </div>

                  <Button
                    type="submit"
                    variant="primary"
                    size="lg"
                    icon="shield"
                    className="w-full sm:w-auto"
                  >
                    Analyze Offer
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>

        {/* Right Column: "What PARAKH checks" Section & Honest Advisory (4 cols) */}
        <div className="lg:col-span-4 space-y-6">
          {/* What PARAKH checks */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/40 shadow-xl space-y-4">
            <div className="pb-3 border-b border-surface-variant/30 flex items-center justify-between">
              <h2 className="font-headline text-base text-on-surface font-bold">
                What PARAKH checks
              </h2>
              <span className="font-mono text-xs text-primary">5-Point Audit</span>
            </div>

            <div className="space-y-3.5">
              <div className="flex items-start gap-3">
                <span className="w-7 h-7 rounded-lg bg-surface-container flex items-center justify-center text-primary text-[16px] shrink-0 mt-0.5">
                  <span className="material-symbols-outlined text-[16px]">description</span>
                </span>
                <div>
                  <h3 className="font-headline-sm text-xs font-semibold text-on-surface">
                    Offer Content
                  </h3>
                  <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                    Scans for upfront payment demands, equipment fees, and coercive deadlines.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <span className="w-7 h-7 rounded-lg bg-surface-container flex items-center justify-center text-secondary text-[16px] shrink-0 mt-0.5">
                  <span className="material-symbols-outlined text-[16px]">badge</span>
                </span>
                <div>
                  <h3 className="font-headline-sm text-xs font-semibold text-on-surface">
                    Recruiter Information
                  </h3>
                  <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                    Flags free webmail handles (@gmail, @yahoo) claiming corporate representation.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <span className="w-7 h-7 rounded-lg bg-surface-container flex items-center justify-center text-tertiary text-[16px] shrink-0 mt-0.5">
                  <span className="material-symbols-outlined text-[16px]">domain</span>
                </span>
                <div>
                  <h3 className="font-headline-sm text-xs font-semibold text-on-surface">
                    Company Information
                  </h3>
                  <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                    Checks consistency between employer identity and sender credentials.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <span className="w-7 h-7 rounded-lg bg-surface-container flex items-center justify-center text-primary text-[16px] shrink-0 mt-0.5">
                  <span className="material-symbols-outlined text-[16px]">link</span>
                </span>
                <div>
                  <h3 className="font-headline-sm text-xs font-semibold text-on-surface">
                    URLs and Domains
                  </h3>
                  <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                    Detects suspicious generic TLDs, link obfuscators, and lookalike domains.
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <span className="w-7 h-7 rounded-lg bg-surface-container flex items-center justify-center text-error text-[16px] shrink-0 mt-0.5">
                  <span className="material-symbols-outlined text-[16px]">warning</span>
                </span>
                <div>
                  <h3 className="font-headline-sm text-xs font-semibold text-on-surface">
                    Suspicious Patterns
                  </h3>
                  <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
                    Flags instant appointments without interviews or routing via Telegram/WhatsApp.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* AI-Assisted Risk Assessment Advisory Card */}
          <div className="rounded-2xl bg-surface-container-low p-6 border border-surface-variant/40 shadow-xl space-y-3">
            <div className="flex items-center gap-2 text-primary font-mono text-xs font-semibold uppercase tracking-wider">
              <span className="material-symbols-outlined text-[18px]">verified_user</span>
              <span>AI-Assisted Risk Assessment</span>
            </div>
            <p className="font-body-sm text-xs text-on-surface-variant leading-relaxed">
              PARAKH produces an explainable risk indicator based on heuristic threat models. Always independently verify offers with the employer before sharing sensitive information or paying any fee.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
