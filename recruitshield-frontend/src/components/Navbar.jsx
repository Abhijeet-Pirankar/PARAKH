import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

function Navbar() {
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isThemeDark, setIsThemeDark] = useState(true);

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  const toggleTheme = () => {
    setIsThemeDark(!isThemeDark);
  };

  return (
    <header className="navbar-wrapper">
      <nav className="navbar" aria-label="Main Navigation">
        {/* Brand Left */}
        <Link to="/" className="nav-brand" onClick={closeMobileMenu} aria-label="PARAKH Home">
          <svg 
            xmlns="http://www.w3.org/2000/svg" 
            width="24" 
            height="24" 
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
          <span className="nav-brand-title">PARAKH</span>
        </Link>

        {/* Center Desktop Links */}
        <div className="nav-links desktop-nav">
          <Link 
            to="/" 
            className={`nav-link ${location.pathname === '/' && !location.hash ? 'active' : ''}`}
          >
            Home
          </Link>
          <a 
            href="/#how-it-works" 
            className={`nav-link ${location.hash === '#how-it-works' ? 'active' : ''}`}
          >
            How It Works
          </a>
          <a 
            href="/#features" 
            className={`nav-link ${location.hash === '#features' ? 'active' : ''}`}
          >
            Features
          </a>
          <a 
            href="/#about" 
            className={`nav-link ${location.hash === '#about' ? 'active' : ''}`}
          >
            About
          </a>
        </div>

        {/* Right Actions */}
        <div className="nav-actions">
          {/* Theme Toggle */}
          <button 
            type="button" 
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={isThemeDark ? "Cybersecurity Dark Shield Active" : "Default Mode"}
            aria-label="Toggle theme appearance"
          >
            {isThemeDark ? (
              <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"></path>
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="4"></circle>
                <path d="M12 2v2"></path><path d="M12 20v2"></path>
                <path d="m4.93 4.93 1.41 1.41"></path><path d="m17.66 17.66 1.41 1.41"></path>
                <path d="M2 12h2"></path><path d="M20 12h2"></path>
                <path d="m6.34 17.66-1.41 1.41"></path><path d="m19.07 4.93-1.41 1.41"></path>
              </svg>
            )}
          </button>

          {/* Desktop Only CTA */}
          <Link 
            to="/check" 
            className="btn btn-outline btn-sm desktop-btn nav-cta-btn"
          >
            Check an Offer
          </Link>

          {/* Mobile Hamburger Button */}
          <button 
            className="mobile-menu-btn" 
            onClick={toggleMobileMenu}
            aria-label="Toggle navigation menu"
          >
            <span className={`hamburger ${isMobileMenuOpen ? 'open' : ''}`}></span>
          </button>
        </div>

        {/* Mobile Slide-Out Drawer */}
        <div className={`mobile-drawer ${isMobileMenuOpen ? 'open' : ''}`}>
          <div className="mobile-drawer-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: '800', fontSize: '1.1rem' }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--brand-blue)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              </svg>
              <span>PARAKH</span>
            </div>
            <button 
              type="button" 
              onClick={closeMobileMenu}
              className="mobile-close-btn"
              aria-label="Close menu"
            >
              ✕
            </button>
          </div>

          <div className="mobile-nav-links">
            <Link 
              to="/" 
              className={`mobile-nav-link ${location.pathname === '/' && !location.hash ? 'active' : ''}`}
              onClick={closeMobileMenu}
            >
              Home
            </Link>
            <a 
              href="/#how-it-works" 
              className="mobile-nav-link"
              onClick={closeMobileMenu}
            >
              How It Works
            </a>
            <a 
              href="/#features" 
              className="mobile-nav-link"
              onClick={closeMobileMenu}
            >
              Features
            </a>
            <a 
              href="/#about" 
              className="mobile-nav-link"
              onClick={closeMobileMenu}
            >
              About
            </a>
          </div>

          <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: '0.75rem', paddingTop: '1.5rem' }}>
            <Link 
              to="/check" 
              className="btn btn-primary"
              onClick={closeMobileMenu}
              style={{ width: '100%', justifyContent: 'center' }}
            >
              Check an Offer
            </Link>
          </div>
        </div>

        {/* Backdrop for mobile drawer */}
        {isMobileMenuOpen && (
          <div 
            className="mobile-drawer-backdrop" 
            onClick={closeMobileMenu}
          />
        )}
      </nav>
    </header>
  );
}

export default Navbar;
