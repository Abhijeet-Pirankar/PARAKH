import React from 'react';
import { Link } from 'react-router-dom';
import Logo from './Logo';

export default function Footer() {
  return (
    <footer className="relative z-10 w-full bg-surface-container-lowest border-t border-surface-variant/40 mt-16">
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8 lg:gap-12">
          {/* Brand & Mission Column (Col 1-2) */}
          <div className="lg:col-span-2 flex flex-col gap-4">
            <Logo className="h-8 w-auto object-contain" />
            <p className="font-body-md text-sm text-on-surface-variant max-w-md leading-relaxed">
              AI-assisted opportunity verification and scam pattern detection safeguarding students and early-career job seekers against fraudulent employment offers.
            </p>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-surface-container-low border border-surface-variant/40 w-fit">
              <span className="material-symbols-outlined text-primary text-[16px]">lock</span>
              <span className="font-mono text-xs text-on-surface-variant">
                Privacy & Security • AI-Assisted Risk Assessment
              </span>
            </div>
          </div>

          {/* Column 3: Platform */}
          <div className="flex flex-col gap-3">
            <span className="font-mono text-xs uppercase tracking-wider text-on-surface font-semibold">
              Platform
            </span>
            <Link
              to="/check"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              Check Offer
            </Link>
            <a
              href="/#features"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              Verification Features
            </a>
            <a
              href="/#pipeline"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              Verification Pipeline
            </a>
            <Link
              to="/report"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              Risk Report
            </Link>
          </div>

          {/* Column 4: Verification Intel */}
          <div className="flex flex-col gap-3">
            <span className="font-mono text-xs uppercase tracking-wider text-on-surface font-semibold">
              Resources
            </span>
            <Link
              to="/check"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              Offer Evaluation
            </Link>
            <a
              href="/#how-it-works"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              How It Works
            </a>
            <a
              href="/#features"
              className="font-body-sm text-sm text-on-surface-variant hover:text-primary transition-colors"
            >
              Scam Pattern Detection
            </a>
          </div>

          {/* Column 5: Trust & Policy */}
          <div className="flex flex-col gap-3">
            <span className="font-mono text-xs uppercase tracking-wider text-on-surface font-semibold">
              Trust & Policy
            </span>
            <span className="font-body-sm text-sm text-on-surface-variant">
              Privacy & Security First
            </span>
            <span className="font-body-sm text-sm text-on-surface-variant">
              Explainable Risk Assessment
            </span>
            <span className="font-body-sm text-sm text-on-surface-variant">
              No Account Required
            </span>
          </div>
        </div>

        {/* Bottom Sub-Bar */}
        <div className="mt-12 pt-6 border-t border-surface-container flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
          <p className="font-mono text-xs text-on-surface-variant">
            © 2025 PARAKH Security Systems. Protecting candidates from fraudulent recruitment schemes.
          </p>
          <div className="flex items-center gap-2 font-mono text-xs text-on-surface-variant">
            <span className="w-2 h-2 rounded-full bg-tertiary" />
            <span>Independent Student Protection Initiative • Verify Before You Trust</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
