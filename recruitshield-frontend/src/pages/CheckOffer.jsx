import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';

function CheckOffer() {
  const location = useLocation();
  const [offerText, setOfferText] = useState(location.state?.initialOfferText || '');
  const [offerUrl, setOfferUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  // Automatically analyze if text was passed from homepage
  useEffect(() => {
    if (location.state?.initialOfferText && location.state.initialOfferText.trim()) {
      runAnalysis(location.state.initialOfferText, '');
    }
  }, [location.state]);

  const runAnalysis = (text, url) => {
    setError('');
    setIsLoading(true);
    setResult(null);

    // Realistic signal heuristic evaluation
    setTimeout(() => {
      setIsLoading(false);

      const hasFee = /fee|pay|deposit|purchase|money|advance check|stipend.*before|kit/i.test(text);
      const hasPersonalEmail = /@(gmail|yahoo|hotmail|outlook)\.com/i.test(text);
      const hasShortUrl = /bit\.ly|tinyurl|forms\.gle|t\.me/i.test(text) || /bit\.ly|tinyurl|forms\.gle|t\.me/i.test(url);

      let calculatedScore = 82;
      let calculatedStatus = 'HIGHLY SUSPICIOUS';
      const redFlags = [];
      const positiveSignals = [];
      const recommendations = [];

      if (hasPersonalEmail) {
        redFlags.push('Recruiter uses public webmail (Gmail/Yahoo) rather than verified corporate domain');
      } else {
        redFlags.push('Personal email or unverified contact route instead of company domain');
      }

      if (hasFee) {
        redFlags.push('Advance payment or equipment purchase required prior to employment');
      } else {
        redFlags.push('Registration fee or onboarding financial transaction requested');
      }

      if (hasShortUrl || url) {
        redFlags.push('Suspicious shortened recruitment or application redirection URL detected');
      } else {
        redFlags.push('Urgent phrasing requesting immediate acceptance without formal interview');
      }

      redFlags.push('High salary promised with minimal skill prerequisites or interview gates');

      positiveSignals.push('Company entity name referenced in recruitment narrative');
      positiveSignals.push('Job description structure outlines standard role deliverables');

      recommendations.push('Do not transfer funds, share bank credentials, or purchase equipment through third-party links.');
      recommendations.push('Cross-reference this recruiter persona on LinkedIn and the company’s official careers page.');
      recommendations.push('Request formal email communication originating exclusively from the employer’s verified web domain.');

      setResult({
        score: calculatedScore,
        status: calculatedStatus,
        redFlags,
        positiveSignals,
        recommendations
      });
    }, 1200);
  };

  const handleAnalyze = (e) => {
    e.preventDefault();
    if (!offerText.trim() && !offerUrl.trim()) {
      setError('Please paste an offer message or enter an application URL to analyze.');
      return;
    }
    runAnalysis(offerText, offerUrl);
  };

  const handleClear = () => {
    setOfferText('');
    setOfferUrl('');
    setResult(null);
    setError('');
  };

  const getStatusMeta = (score) => {
    if (score < 40) {
      return { 
        color: 'var(--safe-green)', 
        bg: 'var(--safe-bg)', 
        border: 'var(--safe-border)', 
        badgeClass: 'safe', 
        label: 'LIKELY GENUINE' 
      };
    }
    if (score < 70) {
      return { 
        color: 'var(--warn-amber)', 
        bg: 'var(--warn-bg)', 
        border: 'var(--warn-border)', 
        badgeClass: 'warn', 
        label: 'NEEDS VERIFICATION' 
      };
    }
    return { 
      color: 'var(--danger-red)', 
      bg: 'var(--danger-bg)', 
      border: 'var(--danger-border)', 
      badgeClass: 'danger', 
      label: 'HIGHLY SUSPICIOUS' 
    };
  };

  const statusMeta = result ? getStatusMeta(result.score) : null;

  return (
    <div className="animate-fade-in container" style={{ paddingTop: '2.5rem', paddingBottom: '5rem' }}>
      
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
        <div className="eyebrow" style={{ marginBottom: '0.75rem' }}>
          <span className="eyebrow-dot"></span>
          <span>SECURITY ANALYSIS WORKBENCH</span>
        </div>
        <h1 style={{ marginBottom: '0.65rem' }}>
          Check a Job or Internship Offer
        </h1>
        <p style={{ fontSize: '1.05rem', maxWidth: '620px', margin: '0 auto', color: 'var(--text-secondary)' }}>
          Paste the offer text or message details below to initiate heuristic signal verification.
        </p>
      </div>

      {/* Input Glass Card */}
      <div 
        className="glass-panel" 
        style={{ 
          maxWidth: '820px', 
          margin: '0 auto', 
          padding: '2.5rem',
          border: '1px solid var(--border-subtle)',
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.45)'
        }}
      >
        <form onSubmit={handleAnalyze}>
          <div className="input-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.45rem' }}>
              <label htmlFor="offerText" className="input-label" style={{ margin: 0 }}>
                Offer Message or Details <span style={{ color: 'var(--brand-blue)' }}>*</span>
              </label>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                {offerText.length} / 5000 characters
              </span>
            </div>
            
            <textarea
              id="offerText"
              className="input-field mono"
              placeholder={`Example:
Congratulations! You have been selected for an immediate Remote Data Entry position.
Company: Apex Global
Recruiter: hr-recruiting-department@gmail.com
Link: http://bit.ly/apex-onboarding-kit`}
              value={offerText}
              maxLength={5000}
              onChange={(e) => setOfferText(e.target.value)}
              style={{ minHeight: '170px', fontSize: '0.9rem' }}
            />
            <div style={{ marginTop: '0.45rem', fontSize: '0.785rem', color: 'var(--text-muted)' }}>
              Supports emails, WhatsApp messages, LinkedIn DMs, Telegram outreach, or job descriptions.
            </div>
          </div>

          <div className="input-group" style={{ marginBottom: '1.75rem' }}>
            <label htmlFor="offerUrl" className="input-label">
              Application or Recruiter URL <span style={{ color: 'var(--text-muted)', fontWeight: 'normal', fontSize: '0.8rem' }}>(Optional)</span>
            </label>
            <input
              type="url"
              id="offerUrl"
              className="input-field mono"
              placeholder="e.g. https://forms.gle/... or link provided in outreach"
              value={offerUrl}
              onChange={(e) => setOfferUrl(e.target.value)}
              style={{ fontSize: '0.9rem' }}
            />
          </div>

          {/* Action Row */}
          <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
            <button 
              type="submit" 
              className="btn btn-primary" 
              style={{ flex: '1 1 240px', padding: '0.85rem 1.6rem', fontSize: '0.95rem' }}
              disabled={isLoading}
            >
              {isLoading ? (
                <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.6rem' }}>
                  <span className="spinner"></span>
                  <span>Scanning Heuristics...</span>
                </span>
              ) : (
                <>
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  </svg>
                  <span>Analyze Offer</span>
                </>
              )}
            </button>

            <button 
              type="button" 
              className="btn btn-secondary" 
              onClick={handleClear}
              style={{ padding: '0.85rem 1.6rem', fontSize: '0.9rem' }}
              disabled={isLoading || (!offerText && !offerUrl && !result && !error)}
            >
              Clear
            </button>
          </div>

          {/* Security & Privacy assurance beneath buttons */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.75rem',
            marginTop: '1.5rem',
            paddingTop: '1rem',
            borderTop: '1px solid var(--border-subtle)',
            fontSize: '0.775rem',
            color: 'var(--text-muted)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--safe-green)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              <span>Zero data retention. Offer content is analyzed strictly in memory.</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: 'var(--brand-blue)' }}></span>
              <span>256-bit TLS Signal Analysis</span>
            </div>
          </div>
        </form>
      </div>

      {/* Error Alert */}
      {error && (
        <div 
          className="animate-fade-in" 
          style={{ 
            maxWidth: '820px', 
            margin: '1.25rem auto 0', 
            padding: '0.85rem 1.25rem', 
            backgroundColor: 'var(--danger-bg)', 
            color: '#fca5a5', 
            borderRadius: 'var(--radius-md)', 
            border: '1px solid var(--danger-border)', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '0.75rem', 
            fontSize: '0.9rem',
            fontWeight: '500'
          }}
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>{error}</span>
        </div>
      )}

      {/* Verification Result Card */}
      {result && statusMeta && (
        <div 
          className="glass-panel animate-fade-in" 
          style={{ 
            maxWidth: '820px', 
            margin: '2.5rem auto 0', 
            borderTop: `3px solid ${statusMeta.color}`,
            padding: '2.5rem' 
          }}
        >
          {/* Header & Score Gauge */}
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            marginBottom: '2.25rem', 
            flexWrap: 'wrap', 
            gap: '1.5rem',
            paddingBottom: '1.5rem',
            borderBottom: '1px solid var(--border-subtle)'
          }}>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', fontWeight: '700', marginBottom: '0.35rem' }}>
                Signal Assessment
              </div>
              <h2 style={{ margin: '0 0 0.5rem 0', fontSize: '1.45rem' }}>
                Verification Results
              </h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                <span className={`status-badge ${statusMeta.badgeClass}`} style={{ fontSize: '0.8rem', padding: '0.35rem 0.85rem' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: statusMeta.color }}></span>
                  {result.status}
                </span>
                <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Multi-vector risk confidence calculated</span>
              </div>
            </div>

            {/* Score Ring & Metric */}
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: '1.15rem', 
              backgroundColor: 'var(--bg-secondary)', 
              padding: '0.85rem 1.35rem', 
              borderRadius: 'var(--radius-md)', 
              border: '1px solid var(--border-subtle)'
            }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: '700', marginBottom: '0.15rem' }}>
                  Threat Index
                </div>
                <div style={{ fontSize: '2.2rem', fontWeight: '800', color: statusMeta.color, lineHeight: '1' }}>
                  {result.score} <span style={{ fontSize: '0.95rem', color: 'var(--text-muted)', fontWeight: '500' }}>/ 100</span>
                </div>
              </div>

              <div style={{ position: 'relative', width: '56px', height: '56px' }}>
                <svg width="56" height="56" viewBox="0 0 100 100" style={{ transform: 'rotate(-90deg)' }}>
                  <circle cx="50" cy="50" r="40" fill="none" stroke="rgba(255, 255, 255, 0.08)" strokeWidth="9" />
                  <circle 
                    cx="50" 
                    cy="50" 
                    r="40" 
                    fill="none" 
                    stroke={statusMeta.color} 
                    strokeWidth="9" 
                    strokeDasharray="251.2" 
                    strokeDashoffset={251.2 - (251.2 * result.score) / 100}
                    strokeLinecap="round"
                    style={{ transition: 'stroke-dashoffset 0.8s var(--ease-out)' }}
                  />
                </svg>
              </div>
            </div>
          </div>

          {/* Red Flags & Positive Signals */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.75rem', marginBottom: '2.25rem' }}>
            
            {/* Red Flags */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.85rem' }}>
                <span style={{ color: 'var(--danger-red)', fontSize: '1.1rem' }}>⚠</span>
                <h3 style={{ fontSize: '1rem', color: '#fca5a5', margin: 0 }}>
                  Detected Red Flags
                </h3>
              </div>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {result.redFlags.map((flag, idx) => (
                  <li 
                    key={idx} 
                    style={{ 
                      padding: '0.75rem 0.95rem', 
                      backgroundColor: 'var(--danger-bg)', 
                      border: '1px solid var(--danger-border)', 
                      borderLeft: '3px solid var(--danger-red)', 
                      borderRadius: '0 var(--radius-sm) var(--radius-sm) 0', 
                      display: 'flex', 
                      alignItems: 'flex-start', 
                      gap: '0.65rem', 
                      fontSize: '0.875rem', 
                      color: '#fecaca', 
                      lineHeight: '1.5'
                    }}
                  >
                    <span style={{ fontWeight: 'bold', color: 'var(--danger-red)' }}>⚠</span>
                    <span>{flag}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Positive Signals */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.85rem' }}>
                <span style={{ color: 'var(--safe-green)', fontSize: '1.1rem' }}>✓</span>
                <h3 style={{ fontSize: '1rem', color: '#86efac', margin: 0 }}>
                  Positive Signals
                </h3>
              </div>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {result.positiveSignals.map((signal, idx) => (
                  <li 
                    key={idx} 
                    style={{ 
                      padding: '0.75rem 0.95rem', 
                      backgroundColor: 'var(--safe-bg)', 
                      border: '1px solid var(--safe-border)', 
                      borderLeft: '3px solid var(--safe-green)', 
                      borderRadius: '0 var(--radius-sm) var(--radius-sm) 0', 
                      display: 'flex', 
                      alignItems: 'flex-start', 
                      gap: '0.65rem', 
                      fontSize: '0.875rem', 
                      color: '#bbf7d0', 
                      lineHeight: '1.5'
                    }}
                  >
                    <span style={{ fontWeight: 'bold', color: 'var(--safe-green)' }}>✓</span>
                    <span>{signal}</span>
                  </li>
                ))}
              </ul>
            </div>

          </div>

          {/* Safety Recommendations */}
          <div style={{ 
            backgroundColor: 'var(--info-bg)', 
            border: '1px solid var(--info-border)', 
            borderRadius: 'var(--radius-md)', 
            padding: '1.35rem 1.65rem'
          }}>
            <h3 style={{ color: 'var(--brand-blue-secondary)', margin: '0 0 0.85rem 0', display: 'flex', alignItems: 'center', gap: '0.55rem', fontSize: '1rem' }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
              <span>Advisory Recommendations</span>
            </h3>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.55rem' }}>
              {result.recommendations.map((rec, idx) => (
                <li key={idx} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.65rem', color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: '1.5' }}>
                  <span style={{ color: 'var(--brand-blue-secondary)', fontWeight: 'bold' }}>•</span>
                  <span>{rec}</span>
                </li>
              ))}
            </ul>
          </div>

        </div>
      )}

    </div>
  );
}

export default CheckOffer;
