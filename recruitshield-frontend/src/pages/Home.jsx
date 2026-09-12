import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

function Home() {
  const navigate = useNavigate();
  const [offerText, setOfferText] = useState('');

  const handleAnalyze = (e) => {
    e.preventDefault();
    navigate('/check', { state: { initialOfferText: offerText } });
  };

  const scrollToAnalysis = () => {
    const el = document.getElementById('analysis-section');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleQuickPasteSample = () => {
    setOfferText(`Congratulations! You have been selected for an immediate Remote Data Entry position.
Company: Apex Innovations Global
Recruiter: hr-recruiting-department@gmail.com
Stipend: $6,500/month (advance check provided)
Next Steps: Kindly purchase your workspace security kit through http://bit.ly/apex-onboarding-fee`);
  };

  return (
    <div className="animate-fade-in" style={{ paddingBottom: '5rem' }}>
      
      {/* 1. HERO SECTION (Two-Column Desktop: Left = Hero Pitch & Trust, Right = Security Verification Visual) */}
      <section className="container" style={{ paddingTop: '3.5rem', paddingBottom: '3.5rem' }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '3.5rem',
          alignItems: 'center'
        }}>
          
          {/* Left Column: Eyebrow, Heading, Description, Trust Indicators, CTAs */}
          <div className="hero-content-col">
            {/* Small Eyebrow */}
            <div className="eyebrow">
              <span className="eyebrow-dot"></span>
              <span>AI-ASSISTED OPPORTUNITY VERIFICATION</span>
            </div>

            {/* Large Heading */}
            <h1>
              VERIFY BEFORE<br />
              <span className="blue-accent">YOU TRUST.</span>
            </h1>

            {/* Supporting Text */}
            <p className="hero-description">
              PARAKH helps students and job seekers identify fake or risky job and internship opportunities before they trust or apply.
            </p>

            {/* Three Concise Trust Indicators */}
            <div className="trust-indicators">
              <div className="trust-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>AI-Assisted Analysis</span>
              </div>
              <div className="trust-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>Recruiter & Company Checks</span>
              </div>
              <div className="trust-item">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>Risk-Based Verification</span>
              </div>
            </div>

            {/* CTA Buttons */}
            <div className="hero-cta-group">
              <button 
                type="button"
                onClick={scrollToAnalysis}
                className="btn btn-primary" 
                style={{ padding: '0.85rem 1.85rem', fontSize: '0.95rem', borderRadius: 'var(--radius-md)' }}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                <span>Start Verifying</span>
              </button>

              <a 
                href="#how-it-works" 
                className="btn btn-secondary" 
                style={{ padding: '0.85rem 1.85rem', fontSize: '0.95rem', borderRadius: 'var(--radius-md)' }}
              >
                <span>How It Works</span>
              </a>
            </div>
          </div>

          {/* Right Column: Hero Security Visual (translucent shield + 5 connected verification nodes) */}
          <div className="glass-panel security-visual-wrap" aria-label="Security Verification Topology">
            <div className="security-visual-bg"></div>
            <div className="security-visual-inner-ring"></div>

            {/* Thin Connecting Lines SVG */}
            <svg className="node-connectors-svg" viewBox="0 0 400 380">
              {/* Lines from center (200, 190) to node positions */}
              {/* Node Recruiter: top center */}
              <line x1="200" y1="135" x2="200" y2="48" stroke="rgba(59, 130, 246, 0.45)" strokeWidth="1.2" strokeDasharray="3 3" />
              {/* Node Company: top right */}
              <line x1="242" y1="176" x2="305" y2="155" stroke="rgba(59, 130, 246, 0.45)" strokeWidth="1.2" strokeDasharray="3 3" />
              {/* Node Link: bottom right */}
              <line x1="228" y1="228" x2="265" y2="280" stroke="rgba(59, 130, 246, 0.45)" strokeWidth="1.2" strokeDasharray="3 3" />
              {/* Node Risk: bottom left */}
              <line x1="172" y1="228" x2="135" y2="280" stroke="rgba(59, 130, 246, 0.45)" strokeWidth="1.2" strokeDasharray="3 3" />
              {/* Node Offer: top left */}
              <line x1="158" y1="176" x2="95" y2="155" stroke="rgba(59, 130, 246, 0.45)" strokeWidth="1.2" strokeDasharray="3 3" />
            </svg>

            {/* Center Translucent Shield */}
            <div className="shield-core">
              <svg 
                xmlns="http://www.w3.org/2000/svg" 
                width="34" 
                height="34" 
                viewBox="0 0 24 24" 
                fill="none" 
                stroke="var(--brand-blue)" 
                strokeWidth="2" 
                strokeLinecap="round" 
                strokeLinejoin="round"
              >
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                <path d="m9 12 2 2 4-4" stroke="var(--brand-blue-secondary)" strokeWidth="2.2"></path>
              </svg>
              <span style={{ fontSize: '0.6rem', fontWeight: '700', color: 'var(--text-secondary)', marginTop: '0.35rem', letterSpacing: '0.02em' }}>
                PARAKH
              </span>
            </div>

            {/* 5 Verification Nodes */}
            <div className="verification-node node-recruiter">
              <span className="verification-node-dot"></span>
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-secondary)" strokeWidth="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              <span>Recruiter</span>
            </div>

            <div className="verification-node node-company">
              <span className="verification-node-dot"></span>
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-secondary)" strokeWidth="2"><rect x="4" y="2" width="16" height="20" rx="2"></rect><line x1="9" y1="6" x2="9.01" y2="6"></line><line x1="15" y1="6" x2="15.01" y2="6"></line><line x1="9" y1="10" x2="9.01" y2="10"></line><line x1="15" y1="10" x2="15.01" y2="10"></line></svg>
              <span>Company</span>
            </div>

            <div className="verification-node node-link">
              <span className="verification-node-dot"></span>
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-secondary)" strokeWidth="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>
              <span>URL</span>
            </div>

            <div className="verification-node node-risk">
              <span className="verification-node-dot" style={{ backgroundColor: 'var(--brand-blue)' }}></span>
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-secondary)" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
              <span>Risk Score</span>
            </div>

            <div className="verification-node node-offer">
              <span className="verification-node-dot"></span>
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-secondary)" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
              <span>Offer</span>
            </div>

            <div style={{
              position: 'absolute',
              bottom: '12px',
              textAlign: 'center',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              letterSpacing: '0.02em',
              fontWeight: '500'
            }}>
              Multi-signal heuristic correlation engine
            </div>
          </div>

        </div>
      </section>

      {/* 2. MAIN ANALYSIS CARD (THE MOST IMPORTANT UI ELEMENT - Centerpiece) */}
      <section id="analysis-section" className="container" style={{ paddingBottom: '3.5rem' }}>
        <div 
          className="glass-panel" 
          style={{ 
            maxWidth: '920px', 
            margin: '0 auto', 
            padding: '2.5rem',
            border: '1px solid rgba(59, 130, 246, 0.22)',
            boxShadow: '0 24px 70px rgba(0, 0, 0, 0.5)'
          }}
        >
          {/* Card Top Label / Header */}
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            flexWrap: 'wrap', 
            gap: '1rem',
            marginBottom: '1.5rem',
            paddingBottom: '1rem',
            borderBottom: '1px solid var(--border-subtle)'
          }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.55rem', marginBottom: '0.35rem' }}>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                <h2 style={{ fontSize: '1.45rem', color: 'var(--text-primary)' }}>
                  Check a Job or Internship Offer
                </h2>
              </div>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', margin: 0 }}>
                Paste the job offer, recruiter message, link, or offer details.
              </p>
            </div>

            {/* Quick Sample Action */}
            <button
              type="button"
              onClick={handleQuickPasteSample}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '0.78rem' }}
            >
              Load Sample Offer
            </button>
          </div>

          <form onSubmit={handleAnalyze}>
            <div className="input-group" style={{ marginBottom: '0.75rem' }}>
              <textarea
                className="input-field mono"
                placeholder={`Example:
Congratulations! You have been selected...
Company: ...
Recruiter: ...
Link: ...`}
                value={offerText}
                maxLength={5000}
                onChange={(e) => setOfferText(e.target.value)}
                style={{ 
                  minHeight: '170px',
                  fontSize: '0.9rem',
                  lineHeight: '1.6',
                  backgroundColor: 'var(--bg-secondary)',
                  border: '1px solid var(--border-subtle)'
                }}
              />
            </div>

            {/* Bottom Row: 0 / 5000 chars | Attach/Upload | Secure Analysis | Analyze Offer button */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '1.25rem',
              paddingTop: '0.5rem'
            }}>
              {/* Left group: Chars + Attach + Secure Badge */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  {offerText.length} / 5000 characters
                </span>

                <button 
                  type="button"
                  onClick={() => alert('File upload parser (PDF/PNG screenshot analysis) will be enabled in next release.')}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--brand-blue-secondary)',
                    fontSize: '0.825rem',
                    fontWeight: '600',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path>
                  </svg>
                  <span>Attach / Upload</span>
                </button>

                {/* Secure Analysis Badge */}
                <div style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  fontSize: '0.775rem',
                  color: 'var(--text-muted)',
                  backgroundColor: 'rgba(255, 255, 255, 0.03)',
                  padding: '0.25rem 0.65rem',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--border-subtle)'
                }}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--safe-green)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                  </svg>
                  <span>Secure Analysis</span>
                </div>
              </div>

              {/* Right Action: Analyze Offer (Strongest Blue Element) */}
              <button 
                type="submit" 
                className="btn btn-primary" 
                style={{ 
                  padding: '0.85rem 1.85rem', 
                  fontSize: '0.95rem',
                  borderRadius: 'var(--radius-md)',
                  boxShadow: '0 4px 20px rgba(59, 130, 246, 0.35)'
                }}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <span>Analyze Offer</span>
              </button>
            </div>
          </form>
        </div>
      </section>

      {/* 3. VERIFICATION FLOW (Horizontal Workflow Container) */}
      <section id="how-it-works" className="container" style={{ paddingBottom: '4.5rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div className="eyebrow" style={{ marginBottom: '0.75rem' }}>
            <span>VERIFICATION PIPELINE</span>
          </div>
          <h2 style={{ marginBottom: '0.5rem' }}>How Verification Works</h2>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
            Sequential multi-vector threat validation across every critical opportunity signal
          </p>
        </div>

        <div className="glass-panel" style={{ padding: '1.75rem 2rem' }}>
          <div className="flow-steps-container">
            
            {/* Step 01: Recruiter */}
            <div className="flow-step-item">
              <div className="flow-step-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </div>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: '700', color: 'var(--brand-blue-secondary)', letterSpacing: '0.04em' }}>01 RECRUITER</div>
                <div style={{ fontWeight: '600', fontSize: '0.9rem', color: 'var(--text-primary)' }}>Verify Recruiter</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Verify recruiter identity</div>
              </div>
            </div>

            <div className="flow-connector-arrow">→</div>

            {/* Step 02: Company */}
            <div className="flow-step-item">
              <div className="flow-step-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="4" y="2" width="16" height="20" rx="2"></rect>
                  <line x1="9" y1="6" x2="9.01" y2="6"></line>
                  <line x1="15" y1="6" x2="15.01" y2="6"></line>
                  <line x1="9" y1="10" x2="9.01" y2="10"></line>
                  <line x1="15" y1="10" x2="15.01" y2="10"></line>
                </svg>
              </div>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: '700', color: 'var(--brand-blue-secondary)', letterSpacing: '0.04em' }}>02 COMPANY</div>
                <div style={{ fontWeight: '600', fontSize: '0.9rem', color: 'var(--text-primary)' }}>Validate Company</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Validate company details</div>
              </div>
            </div>

            <div className="flow-connector-arrow">→</div>

            {/* Step 03: Link */}
            <div className="flow-step-item">
              <div className="flow-step-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                  <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                </svg>
              </div>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: '700', color: 'var(--brand-blue-secondary)', letterSpacing: '0.04em' }}>03 LINK</div>
                <div style={{ fontWeight: '600', fontSize: '0.9rem', color: 'var(--text-primary)' }}>Check URL Safety</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Check URL safety</div>
              </div>
            </div>

            <div className="flow-connector-arrow">→</div>

            {/* Step 04: Offer */}
            <div className="flow-step-item">
              <div className="flow-step-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                </svg>
              </div>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: '700', color: 'var(--brand-blue-secondary)', letterSpacing: '0.04em' }}>04 OFFER</div>
                <div style={{ fontWeight: '600', fontSize: '0.9rem', color: 'var(--text-primary)' }}>Analyze Offer</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Analyze offer content</div>
              </div>
            </div>

            <div className="flow-connector-arrow">→</div>

            {/* Step 05: Risk Score */}
            <div className="flow-step-item">
              <div className="flow-step-icon" style={{ background: 'var(--brand-blue-subtle)', borderColor: 'var(--brand-blue)' }}>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
              </div>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: '700', color: 'var(--brand-blue)', letterSpacing: '0.04em' }}>05 RISK SCORE</div>
                <div style={{ fontWeight: '700', fontSize: '0.9rem', color: 'var(--text-primary)' }}>Risk Assessment</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Understand the risk</div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* 4. WHY PARAKH (Features Section with Asymmetric Visual Hierarchy) */}
      <section id="features" className="container" style={{ paddingBottom: '4.5rem' }}>
        <div style={{ marginBottom: '2.5rem' }}>
          <div className="eyebrow" style={{ marginBottom: '0.75rem' }}>
            <span>SECURITY ARCHITECTURE</span>
          </div>
          <h2 style={{ marginBottom: '0.5rem' }}>WHY PARAKH?</h2>
          <p style={{ fontSize: '1rem', color: 'var(--text-muted)', maxWidth: '600px' }}>
            Multiple layers of verification before you trust an opportunity.
          </p>
        </div>

        {/* Asymmetric Grid: 1 Large Feature Card + 4 Smaller Supporting Cards */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(12, 1fr)',
          gap: '1.5rem'
        }}>
          
          {/* 1 Larger Feature Card (Span 12 on mobile/tablet, Span 6 on desktop) */}
          <div 
            className="glass-card" 
            style={{ 
              gridColumn: 'span 12',
              padding: '2.5rem',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              borderLeft: '3px solid var(--brand-blue)'
            }}
          >
            <div>
              <div style={{ 
                width: '44px', 
                height: '44px', 
                borderRadius: 'var(--radius-md)', 
                backgroundColor: 'var(--brand-blue-subtle)', 
                border: '1px solid var(--brand-blue-border)', 
                color: 'var(--brand-blue-secondary)',
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                marginBottom: '1.5rem'
              }}>
                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 2a4 4 0 0 0-4 4v1a4 4 0 0 0-2 3.5 4 4 0 0 0 2 3.5v1a4 4 0 0 0 4 4 4 4 0 0 0 4-4v-1a4 4 0 0 0 2-3.5 4 4 0 0 0-2-3.5V6a4 4 0 0 0-4-4z"></path>
                  <path d="M8 18a4 4 0 0 0 8 0"></path>
                </svg>
              </div>

              <div style={{ fontSize: '0.75rem', fontWeight: '700', color: 'var(--brand-blue-secondary)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                Primary Defense Engine
              </div>
              <h3 style={{ fontSize: '1.45rem', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>
                AI-Powered Threat Analysis
              </h3>
              <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: '1.65', marginBottom: '1.5rem', maxWidth: '540px' }}>
                PARAKH dissects recruitment outreach through layered linguistic models, detecting false authority, artificial urgency, payment advance traps, and unverified sender impersonations before users respond.
              </p>
            </div>

            {/* Heuristic signals strip */}
            <div style={{ 
              display: 'flex', 
              flexWrap: 'wrap', 
              gap: '0.6rem', 
              paddingTop: '1rem', 
              borderTop: '1px solid var(--border-subtle)' 
            }}>
              <span className="status-badge info">Grammar & Phishing Heuristics</span>
              <span className="status-badge info">Check Advance Scams</span>
              <span className="status-badge info">Domain Spoofing Check</span>
            </div>
          </div>

          {/* 4 Smaller Supporting Cards (Grid 2x2 or 4 cols) */}
          {/* Card 1: Risk Scoring System */}
          <div className="glass-card" style={{ gridColumn: 'span 6', padding: '1.75rem' }}>
            <div style={{ 
              width: '38px', 
              height: '38px', 
              borderRadius: 'var(--radius-md)', 
              backgroundColor: 'rgba(255, 255, 255, 0.04)', 
              border: '1px solid var(--border-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem'
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.45rem' }}>Risk Scoring System</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              Translates multi-source evidence into an objective 0–100 security threat scale with clear categorization.
            </p>
          </div>

          {/* Card 2: Offer Verification */}
          <div className="glass-card" style={{ gridColumn: 'span 6', padding: '1.75rem' }}>
            <div style={{ 
              width: '38px', 
              height: '38px', 
              borderRadius: 'var(--radius-md)', 
              backgroundColor: 'rgba(255, 255, 255, 0.04)', 
              border: '1px solid var(--border-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem'
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.45rem' }}>Offer Verification</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              Evaluates role compensation reality, equipment fees, registration demands, and vague job deliverables.
            </p>
          </div>

          {/* Card 3: Link Protection */}
          <div className="glass-card" style={{ gridColumn: 'span 6', padding: '1.75rem' }}>
            <div style={{ 
              width: '38px', 
              height: '38px', 
              borderRadius: 'var(--radius-md)', 
              backgroundColor: 'rgba(255, 255, 255, 0.04)', 
              border: '1px solid var(--border-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem'
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.45rem' }}>Link Protection</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              Examines shortened URLs, form links, credential harvesters, and lookalike domain registries.
            </p>
          </div>

          {/* Card 4: Community Reports */}
          <div className="glass-card" style={{ gridColumn: 'span 6', padding: '1.75rem' }}>
            <div style={{ 
              width: '38px', 
              height: '38px', 
              borderRadius: 'var(--radius-md)', 
              backgroundColor: 'rgba(255, 255, 255, 0.04)', 
              border: '1px solid var(--border-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem'
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.45rem' }}>Community Reports</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              Cross-references recurring scam templates and fraudulent recruiter personas reported by other job seekers.
            </p>
          </div>

        </div>
      </section>

      {/* 5. SAMPLE VERIFICATION RESULT (Realistic Product Preview) */}
      <section className="container" style={{ paddingBottom: '4.5rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2.25rem' }}>
          <div className="eyebrow" style={{ marginBottom: '0.75rem' }}>
            <span>LIVE DETECTION PREVIEW</span>
          </div>
          <h2 style={{ marginBottom: '0.5rem' }}>SAMPLE VERIFICATION RESULT</h2>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
            Real-world breakdown of multi-signal heuristics and actionable advisory output.
          </p>
        </div>

        <div 
          className="glass-panel" 
          style={{ 
            maxWidth: '820px', 
            margin: '0 auto', 
            padding: '2.5rem',
            borderTop: '3px solid var(--warn-amber)' 
          }}
        >
          {/* Header Row: Score + Status Badge */}
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            flexWrap: 'wrap', 
            gap: '1.5rem', 
            marginBottom: '2rem',
            paddingBottom: '1.5rem',
            borderBottom: '1px solid var(--border-subtle)'
          }}>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', fontWeight: '700', marginBottom: '0.35rem' }}>
                Verification Status
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span className="status-badge warn" style={{ fontSize: '0.825rem', padding: '0.35rem 0.85rem' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--warn-amber)' }}></span>
                  NEEDS VERIFICATION
                </span>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Heuristics flagged 2 anomalies</span>
              </div>
            </div>

            {/* Risk Score Pill */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '1rem',
              backgroundColor: 'var(--bg-secondary)',
              padding: '0.75rem 1.25rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)'
            }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: '700' }}>
                  Risk Score
                </div>
                <div style={{ fontSize: '1.85rem', fontWeight: '800', color: 'var(--warn-amber)', lineHeight: '1' }}>
                  72 <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', fontWeight: '500' }}>/ 100</span>
                </div>
              </div>
              {/* Circular Gauge Preview */}
              <div style={{ position: 'relative', width: '48px', height: '48px' }}>
                <svg width="48" height="48" viewBox="0 0 100 100" style={{ transform: 'rotate(-90deg)' }}>
                  <circle cx="50" cy="50" r="40" fill="none" stroke="rgba(255, 255, 255, 0.08)" strokeWidth="10" />
                  <circle 
                    cx="50" 
                    cy="50" 
                    r="40" 
                    fill="none" 
                    stroke="var(--warn-amber)" 
                    strokeWidth="10" 
                    strokeDasharray="251.2" 
                    strokeDashoffset={251.2 - (251.2 * 72) / 100}
                    strokeLinecap="round"
                  />
                </svg>
              </div>
            </div>
          </div>

          {/* Signals Breakdown: Red Flags & Safe Signals */}
          <div style={{ marginBottom: '1.75rem' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: '0.85rem' }}>
              Detected Signals
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
              {/* Flag 1 */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--danger-bg)',
                border: '1px solid var(--danger-border)',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.875rem',
                color: '#fca5a5'
              }}>
                <span style={{ fontWeight: 'bold' }}>⚠</span>
                <span>Recruiter email does not match company domain</span>
              </div>

              {/* Flag 2 */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--danger-bg)',
                border: '1px solid var(--danger-border)',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.875rem',
                color: '#fca5a5'
              }}>
                <span style={{ fontWeight: 'bold' }}>⚠</span>
                <span>Suspicious shortened URL detected</span>
              </div>

              {/* Safe Signal */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--safe-bg)',
                border: '1px solid var(--safe-border)',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.875rem',
                color: '#86efac'
              }}>
                <span style={{ fontWeight: 'bold' }}>✓</span>
                <span>Company information found</span>
              </div>
            </div>
          </div>

          {/* Recommendation Box */}
          <div style={{
            backgroundColor: 'var(--info-bg)',
            border: '1px solid var(--info-border)',
            borderRadius: 'var(--radius-md)',
            padding: '1.1rem 1.35rem',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.75rem'
          }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue-secondary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, marginTop: '2px' }}>
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--brand-blue-secondary)', marginBottom: '0.2rem' }}>
                Recommendation
              </div>
              <div style={{ fontSize: '0.9rem', color: 'var(--text-primary)', lineHeight: '1.5' }}>
                Verify the recruiter through the company's official website before proceeding.
              </div>
            </div>
          </div>

        </div>
      </section>

      {/* 6. SAFETY SECTION ("YOUR SAFETY COMES FIRST.") */}
      <section id="about" className="container" style={{ paddingBottom: '4.5rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
          <div className="eyebrow" style={{ marginBottom: '0.75rem' }}>
            <span>ETHICAL COMMITMENT</span>
          </div>
          <h2 style={{ marginBottom: '0.5rem' }}>YOUR SAFETY COMES FIRST.</h2>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
            Transparent, privacy-preserving cybersecurity principles designed for early-career professionals.
          </p>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
          gap: '1.5rem',
          maxWidth: '1080px',
          margin: '0 auto'
        }}>
          {/* Point 1 */}
          <div className="glass-card" style={{ padding: '1.75rem' }}>
            <div style={{ 
              width: '36px', 
              height: '36px', 
              borderRadius: 'var(--radius-sm)', 
              backgroundColor: 'var(--brand-blue-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem' 
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.05rem', marginBottom: '0.45rem' }}>Privacy-First Analysis</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              Pasted job details and recruiter emails are scanned strictly in memory without being archived, sold, or shared with third parties.
            </p>
          </div>

          {/* Point 2 */}
          <div className="glass-card" style={{ padding: '1.75rem' }}>
            <div style={{ 
              width: '36px', 
              height: '36px', 
              borderRadius: 'var(--radius-sm)', 
              backgroundColor: 'var(--brand-blue-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem' 
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.05rem', marginBottom: '0.45rem' }}>Secure Processing</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              End-to-end encrypted transport layer protects all verification lookups against intercept or unauthorized inspection.
            </p>
          </div>

          {/* Point 3 */}
          <div className="glass-card" style={{ padding: '1.75rem' }}>
            <div style={{ 
              width: '36px', 
              height: '36px', 
              borderRadius: 'var(--radius-sm)', 
              backgroundColor: 'var(--brand-blue-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem' 
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.05rem', marginBottom: '0.45rem' }}>Explainable Results</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              No opaque black-box verdicts. Every risk score is accompanied by itemized red flags, safe signals, and verification rationale.
            </p>
          </div>

          {/* Point 4 */}
          <div className="glass-card" style={{ padding: '1.75rem' }}>
            <div style={{ 
              width: '36px', 
              height: '36px', 
              borderRadius: 'var(--radius-sm)', 
              backgroundColor: 'var(--brand-blue-subtle)', 
              color: 'var(--brand-blue-secondary)',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              marginBottom: '1rem' 
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
            </div>
            <h3 style={{ fontSize: '1.05rem', marginBottom: '0.45rem' }}>No Guaranteed Claims</h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: '1.55', margin: 0 }}>
              PARAKH provides AI-assisted probability analysis rather than legal warranty. We empower you to perform independent checks.
            </p>
          </div>
        </div>
      </section>

      {/* 7. FINAL CTA */}
      <section className="container" style={{ paddingBottom: '2rem' }}>
        <div 
          className="glass-panel" 
          style={{ 
            maxWidth: '920px', 
            margin: '0 auto', 
            padding: '3.5rem 2.5rem', 
            textAlign: 'center',
            position: 'relative',
            overflow: 'hidden'
          }}
        >
          {/* Subtle ambient lighting behind CTA */}
          <div style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: '280px',
            height: '280px',
            background: 'radial-gradient(circle, rgba(59, 130, 246, 0.12) 0%, transparent 70%)',
            pointerEvents: 'none'
          }}></div>

          <div className="eyebrow" style={{ marginBottom: '1.25rem' }}>
            <span>IMMEDIATE ACCESS</span>
          </div>

          <h2 style={{ fontSize: 'clamp(2rem, 3.5vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem', letterSpacing: '-0.025em' }}>
            BEFORE YOU APPLY.<br />
            <span className="blue-accent">VERIFY.</span>
          </h2>

          <p style={{ fontSize: '1.05rem', color: 'var(--text-secondary)', marginBottom: '2rem', maxWidth: '480px', margin: '0 auto 2rem' }}>
            Check the opportunity before you trust it.
          </p>

          <div>
            <Link 
              to="/check" 
              className="btn btn-primary" 
              style={{ 
                padding: '0.9rem 2.2rem', 
                fontSize: '1rem', 
                borderRadius: 'var(--radius-md)' 
              }}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
              <span>Check an Offer</span>
            </Link>
          </div>
        </div>
      </section>

    </div>
  );
}

export default Home;
