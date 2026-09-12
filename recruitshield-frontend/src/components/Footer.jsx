import React from 'react';
import { Link } from 'react-router-dom';

function Footer() {
  return (
    <footer style={{
      marginTop: 'auto',
      backgroundColor: 'var(--bg-primary)',
      borderTop: '1px solid var(--border-subtle)',
      padding: '3.5rem 1.5rem 2.5rem',
      position: 'relative'
    }}>
      <div className="container" style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        flexWrap: 'wrap',
        gap: '2.5rem',
        paddingBottom: '2.5rem',
        borderBottom: '1px solid rgba(148, 163, 184, 0.08)'
      }}>
        {/* Brand Column */}
        <div style={{ maxWidth: '380px' }}>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '0.6rem', 
            fontSize: '1.2rem', 
            fontWeight: '800', 
            color: 'var(--text-primary)', 
            marginBottom: '0.65rem',
            letterSpacing: '-0.02em'
          }}>
            <svg 
              xmlns="http://www.w3.org/2000/svg" 
              width="22" 
              height="22" 
              viewBox="0 0 24 24" 
              fill="none" 
              stroke="var(--brand-blue)" 
              strokeWidth="2.2" 
              strokeLinecap="round" 
              strokeLinejoin="round"
            >
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              <path d="m9 12 2 2 4-4" stroke="var(--brand-blue-secondary)" strokeWidth="2"></path>
            </svg>
            <span>PARAKH</span>
          </div>
          <p style={{ 
            fontSize: '0.875rem', 
            color: 'var(--text-secondary)', 
            fontWeight: '600',
            marginBottom: '0.45rem'
          }}>
            Verify Before You Trust.
          </p>
          <p style={{ 
            fontSize: '0.85rem', 
            color: 'var(--text-muted)', 
            lineHeight: '1.6', 
            margin: 0 
          }}>
            Protecting students, freshers, and job seekers from recruitment fraud and phishing threats through AI-assisted signal verification.
          </p>
        </div>

        {/* Links Column */}
        <div style={{ display: 'flex', gap: '3rem', flexWrap: 'wrap' }}>
          <div>
            <div style={{ 
              fontSize: '0.75rem', 
              fontWeight: '700', 
              textTransform: 'uppercase', 
              letterSpacing: '0.08em', 
              color: 'var(--text-primary)', 
              marginBottom: '1rem' 
            }}>
              Platform
            </div>
            <ul style={{ 
              listStyle: 'none', 
              padding: 0, 
              margin: 0, 
              display: 'flex', 
              flexDirection: 'column', 
              gap: '0.65rem', 
              fontSize: '0.875rem' 
            }}>
              <li>
                <a href="/#how-it-works" style={{ color: 'var(--text-secondary)', textDecoration: 'none', transition: 'color 0.15s ease' }} onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'} onMouseLeave={(e) => e.target.style.color = 'var(--text-secondary)'}>How It Works</a>
              </li>
              <li>
                <a href="/#features" style={{ color: 'var(--text-secondary)', textDecoration: 'none', transition: 'color 0.15s ease' }} onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'} onMouseLeave={(e) => e.target.style.color = 'var(--text-secondary)'}>Features</a>
              </li>
              <li>
                <a href="/#about" style={{ color: 'var(--text-secondary)', textDecoration: 'none', transition: 'color 0.15s ease' }} onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'} onMouseLeave={(e) => e.target.style.color = 'var(--text-secondary)'}>About</a>
              </li>
              <li>
                <Link to="/check" style={{ color: 'var(--text-secondary)', textDecoration: 'none', transition: 'color 0.15s ease' }} onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'} onMouseLeave={(e) => e.target.style.color = 'var(--text-secondary)'}>Check an Offer</Link>
              </li>
            </ul>
          </div>

          <div>
            <div style={{ 
              fontSize: '0.75rem', 
              fontWeight: '700', 
              textTransform: 'uppercase', 
              letterSpacing: '0.08em', 
              color: 'var(--text-primary)', 
              marginBottom: '1rem' 
            }}>
              Legal & Trust
            </div>
            <ul style={{ 
              listStyle: 'none', 
              padding: 0, 
              margin: 0, 
              display: 'flex', 
              flexDirection: 'column', 
              gap: '0.65rem', 
              fontSize: '0.875rem' 
            }}>
              <li>
                <a href="#privacy" onClick={(e) => { e.preventDefault(); alert('PARAKH Privacy Policy: We do not store personally identifiable submission data.'); }} style={{ color: 'var(--text-secondary)', textDecoration: 'none', transition: 'color 0.15s ease' }} onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'} onMouseLeave={(e) => e.target.style.color = 'var(--text-secondary)'}>Privacy</a>
              </li>
              <li>
                <a href="#terms" onClick={(e) => { e.preventDefault(); alert('PARAKH Terms: AI-assisted signals are advisory and not a legal guarantee.'); }} style={{ color: 'var(--text-secondary)', textDecoration: 'none', transition: 'color 0.15s ease' }} onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'} onMouseLeave={(e) => e.target.style.color = 'var(--text-secondary)'}>Terms</a>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div className="container" style={{
        paddingTop: '1.75rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '1rem',
        color: 'var(--text-muted)',
        fontSize: '0.8rem'
      }}>
        <div>
          &copy; {new Date().getFullYear()} PARAKH. All rights reserved.
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: 'var(--safe-green)' }}></span>
          <span>Security Engine Online</span>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
