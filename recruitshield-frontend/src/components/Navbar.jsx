import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Logo from './Logo';

export default function Navbar() {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/' && !location.hash;
    return location.pathname === path;
  };

  const closeMenu = () => setMobileMenuOpen(false);

  return (
    <header className="fixed top-0 inset-x-0 z-50 bg-surface-container-lowest/85 backdrop-blur-xl border-b border-surface-variant/40">
      <div className="h-16 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        {/* Brand Left */}
        <Link to="/" onClick={closeMenu} className="flex items-center gap-3 shrink-0 focus:outline-none">
          <Logo className="h-8 w-auto object-contain" />
        </Link>

        {/* Desktop Navigation */}
        <nav className="hidden lg:flex items-center gap-6 text-sm font-medium">
          <Link
            to="/"
            className={`transition-colors py-1 ${
              isActive('/')
                ? 'text-primary font-semibold'
                : 'text-on-surface-variant hover:text-on-surface'
            }`}
          >
            Home
          </Link>
          <a
            href="/#how-it-works"
            className="text-on-surface-variant hover:text-on-surface transition-colors py-1"
          >
            How It Works
          </a>
          <a
            href="/#features"
            className="text-on-surface-variant hover:text-on-surface transition-colors py-1"
          >
            Features
          </a>
          <Link
            to="/check"
            className={`transition-colors py-1 ${
              isActive('/check')
                ? 'text-primary font-semibold'
                : 'text-on-surface-variant hover:text-on-surface'
            }`}
          >
            Check Offer
          </Link>
          <Link
            to="/report"
            className={`transition-colors py-1 ${
              isActive('/report')
                ? 'text-primary font-semibold'
                : 'text-on-surface-variant hover:text-on-surface'
            }`}
          >
            Risk Report
          </Link>
        </nav>

        {/* Action Controls Right */}
        <div className="flex items-center gap-3 shrink-0">
          {/* Live Engine Status Pill */}
          <div className="hidden sm:flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-container-low border border-surface-variant/40">
            <span className="relative flex h-2 w-2">
              <span className="relative inline-flex rounded-full h-2 w-2 bg-tertiary" />
            </span>
            <span className="font-mono text-xs text-tertiary">AI-Assisted Analysis</span>
          </div>

          {/* Primary Action Button */}
          <Link
            to="/check"
            className="hidden md:inline-flex items-center justify-center px-4 py-1.5 rounded-xl bg-gradient-to-r from-[#0891b2] to-[#1d4ed8] text-white font-bold text-xs shadow-[0_2px_12px_rgba(6,182,212,0.25)] hover:brightness-110 active:scale-95 transition-all"
          >
            Check an Offer
          </Link>

          {/* Security Shield Icon Avatar */}
          <div className="w-8 h-8 rounded-full bg-primary/20 border border-primary/40 flex items-center justify-center shrink-0 text-primary">
            <span className="material-symbols-outlined text-[18px]">security</span>
          </div>

          {/* Mobile Hamburger Toggle */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="lg:hidden p-2 rounded-lg bg-surface-container text-on-surface hover:bg-surface-bright focus:outline-none"
            aria-label="Toggle navigation menu"
          >
            <span className="material-symbols-outlined text-[22px]">
              {mobileMenuOpen ? 'close' : 'menu'}
            </span>
          </button>
        </div>
      </div>

      {/* Mobile Slide-down Menu */}
      {mobileMenuOpen && (
        <div className="lg:hidden bg-surface-container-lowest border-b border-surface-variant/40 px-4 pt-2 pb-6 space-y-3 shadow-2xl">
          <Link
            to="/"
            onClick={closeMenu}
            className={`block py-2 text-sm font-medium ${
              isActive('/') ? 'text-primary' : 'text-on-surface-variant'
            }`}
          >
            Home
          </Link>
          <a
            href="/#how-it-works"
            onClick={closeMenu}
            className="block py-2 text-sm font-medium text-on-surface-variant hover:text-on-surface"
          >
            How It Works
          </a>
          <a
            href="/#features"
            onClick={closeMenu}
            className="block py-2 text-sm font-medium text-on-surface-variant hover:text-on-surface"
          >
            Features
          </a>
          <Link
            to="/check"
            onClick={closeMenu}
            className={`block py-2 text-sm font-medium ${
              isActive('/check') ? 'text-primary' : 'text-on-surface-variant'
            }`}
          >
            Check Offer
          </Link>
          <Link
            to="/report"
            onClick={closeMenu}
            className={`block py-2 text-sm font-medium ${
              isActive('/report') ? 'text-primary' : 'text-on-surface-variant'
            }`}
          >
            Risk Report
          </Link>
          <div className="pt-2">
            <Link
              to="/check"
              onClick={closeMenu}
              className="w-full inline-flex items-center justify-center px-6 py-3 rounded-xl bg-gradient-to-r from-[#0891b2] to-[#1d4ed8] text-white font-bold text-[18px] shadow-[0_2px_14px_rgba(6,182,212,0.25)] hover:brightness-110 active:scale-95 transition-all"
            >
              Check an Offer
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
