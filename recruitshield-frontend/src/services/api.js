/**
 * PARAKH Backend API Client & Verification Service
 * Connects frontend verification workbench to Spring Boot REST endpoints
 * with support for client-side forensic heuristics and structured error handling.
 */

export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL && import.meta.env.VITE_API_BASE_URL.trim() !== '')
    ? import.meta.env.VITE_API_BASE_URL.trim().replace(/\/+$/, '')
    : 'http://localhost:8080';

/**
 * Structured API Error class encapsulating backend and network failure states.
 */
export class ApiError extends Error {
  constructor(message, status = null, code = 'API_ERROR', details = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = Array.isArray(details) ? details : details ? [details] : [];
  }
}

/**
 * Normalizes input parameters into backend VerifyRequest DTO format.
 *
 * @param {Object} offerData
 * @param {string} [offerData.offerText] - Primary offer communication text
 * @param {string} [offerData.text] - Alternative legacy key for offerText
 * @param {string} [offerData.companyName] - Stated company/employer name
 * @param {string} [offerData.companyWebsite] - Company website or career portal URL
 * @param {string} [offerData.url] - Alternative key for companyWebsite
 * @param {string} [offerData.recruiterEmail] - Sender / recruiter email
 * @param {string} [offerData.email] - Alternative key for recruiterEmail
 * @param {string} [offerData.receivedVia] - Communication channel (WhatsApp, Telegram, etc.)
 * @param {string} [offerData.source] - Alternative key for receivedVia
 * @returns {Object} Structured VerifyRequest payload
 */
export function buildVerifyPayload(offerData = {}) {
  const offerText = (offerData.offerText || offerData.text || '').trim();
  const companyName = (offerData.companyName || '').trim();
  const companyWebsite = (offerData.companyWebsite || offerData.url || '').trim();
  const recruiterEmail = (offerData.recruiterEmail || offerData.email || '').trim();
  const receivedVia = (offerData.receivedVia || offerData.source || '').trim();

  return {
    offerText,
    text: offerText, // legacy compatibility field for backend VerifyRequest(text)
    companyName,
    companyWebsite,
    recruiterEmail,
    receivedVia
  };
}

/**
 * Sends offer details to the Spring Boot risk analysis engine.
 * Primary endpoint: POST /api/analyze-offer
 * Fallback endpoint: POST /api/verify (only on HTTP 404 from primary endpoint)
 *
 * @param {Object} offerData - Input offer data
 * @param {Object} [options] - Optional settings (e.g. allowOfflineFallback, timeoutMs)
 * @returns {Promise<Object>} Verification risk report preserving all backend DTO fields
 */
export async function analyzeOffer(offerData, options = {}) {
  const payload = buildVerifyPayload(offerData);

  const timeoutMs = options.timeoutMs || 8000;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  let response;
  const primaryEndpoint = `${API_BASE_URL}/api/analyze-offer`;

  try {
    response = await fetch(primaryEndpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    });
  } catch (err) {
    // In browser environments with cross-origin calls, an unmapped backend endpoint
    // fails the CORS preflight check and throws a TypeError before returning HTTP 404.
    // If and only if the backend server is verified alive and responsive on /api/verify,
    // treat the /api/analyze-offer failure as an endpoint unavailability (404) for compatibility fallback.
    let isCorsEndpointMismatch = false;
    if (typeof window !== 'undefined' && (err?.name === 'TypeError' || err?.message?.includes('fetch'))) {
      try {
        const probe = await fetch(`${API_BASE_URL}/api/verify`, {
          method: 'OPTIONS',
          signal: controller.signal
        });
        if (probe.ok || probe.status === 200 || probe.status === 204) {
          isCorsEndpointMismatch = true;
        }
      } catch {
        // Backend truly unreachable or server connection refused
      }
    }

    if (isCorsEndpointMismatch) {
      response = { status: 404, ok: false };
    } else {
      clearTimeout(timeoutId);

      if (options.allowOfflineFallback) {
        console.warn('Backend unavailable, using explicit offline fallback as requested:', err.message);
        return analyzeOfferLocally(offerData);
      }

      const networkError = new ApiError(
        `Unable to connect to PARAKH backend service. Please ensure the server is running on ${API_BASE_URL}.`,
        null,
        'BACKEND_UNAVAILABLE'
      );
      networkError.originalError = err;
      throw networkError;
    }
  }

  // Fallback to /api/verify ONLY if the primary endpoint specifically returned HTTP 404
  if (response.status === 404) {
    const fallbackEndpoint = `${API_BASE_URL}/api/verify`;
    try {
      response = await fetch(fallbackEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
    } catch (fallbackErr) {
      clearTimeout(timeoutId);

      if (options.allowOfflineFallback) {
        return analyzeOfferLocally(offerData);
      }

      const networkError = new ApiError(
        `Unable to connect to PARAKH backend service. Please ensure the server is running on ${API_BASE_URL}.`,
        null,
        'BACKEND_UNAVAILABLE'
      );
      networkError.originalError = fallbackErr;
      throw networkError;
    }
  }

  clearTimeout(timeoutId);

  // Handle HTTP error statuses
  if (!response.ok) {
    let errorJson = null;
    try {
      if (typeof response.json === 'function') {
        errorJson = await response.json();
      }
    } catch {
      // response wasn't JSON
    }

    const status = response.status;
    let code = 'API_ERROR';
    let userMessage = errorJson?.message;
    const details = errorJson?.details || [];

    if (status === 400) {
      code = 'VALIDATION_ERROR';
      userMessage = userMessage || 'Invalid offer data provided. Please check the inputs.';
    } else if (status === 401) {
      code = 'AUTH_ERROR';
      userMessage = userMessage || 'Authentication required.';
    } else if (status === 403) {
      code = 'AUTH_ERROR';
      userMessage = userMessage || 'Access forbidden.';
    } else if (status === 404) {
      code = 'NOT_FOUND';
      userMessage = userMessage || 'The requested analysis service endpoint was not found on the server (HTTP 404).';
    } else if (status >= 500) {
      code = 'SERVER_ERROR';
      userMessage = 'The PARAKH analysis service encountered an internal error. Please try again later.';
    } else {
      code = 'HTTP_ERROR';
      userMessage = userMessage || `Request failed with HTTP status ${status}.`;
    }

    throw new ApiError(userMessage, status, code, details);
  }

  const data = await response.json();

  // Normalize scores ensuring both score and riskScore are present
  const score = data.riskScore !== undefined ? data.riskScore : (data.score !== undefined ? data.score : 0);
  const status = data.status || (score >= 70 ? 'HIGHLY_SUSPICIOUS' : score >= 40 ? 'NEEDS_VERIFICATION' : 'LIKELY_GENUINE');
  const riskLevel = data.riskLevel || (score >= 70 ? 'HIGH' : score >= 40 ? 'MEDIUM' : 'LOW');

  // Red flags directly from backend
  const redFlags = Array.isArray(data.redFlags) && data.redFlags.length > 0
    ? data.redFlags
    : (Array.isArray(data.reasons) ? data.reasons : []);

  // Positive signals directly from backend
  const positiveSignals = Array.isArray(data.positiveSignals) ? data.positiveSignals : [];

  // Recommendations directly from backend
  const recommendations = Array.isArray(data.recommendations) && data.recommendations.length > 0
    ? data.recommendations
    : (data.recommendation ? [data.recommendation] : []);

  // Return the actual backend response with all forensic and AI metadata preserved
  return {
    ...data,
    riskScore: score,
    score: score,
    ruleBasedScore: data.ruleBasedScore !== undefined ? data.ruleBasedScore : score,
    aiRiskProbability: data.aiRiskProbability !== undefined ? data.aiRiskProbability : null,
    aiClassification: data.aiClassification || null,
    aiAnalysisAvailable: Boolean(data.aiAnalysisAvailable),
    status: status,
    riskLevel: riskLevel,
    redFlags: redFlags,
    reasons: redFlags,
    positiveSignals: positiveSignals,
    recommendations: recommendations,
    recommendation: data.recommendation || (recommendations[0] || ''),
    analysisSummary: data.analysisSummary || '',
    urlVerification: data.urlVerification || null,
    recruiterVerification: data.recruiterVerification || null,
    source: 'backend-api',
    isOffline: false
  };
}

/**
 * Backward-compatible alias for analyzeOffer
 */
export const verifyOffer = analyzeOffer;

/**
 * Run client-side heuristic fallback mirroring Spring Boot AnalysisService.
 * Explicitly marks result as offline/local analysis.
 */
export function analyzeOfferLocally(offerData = {}) {
  const text = (offerData.offerText || offerData.text || '');
  const url = (offerData.companyWebsite || offerData.url || '');
  const email = (offerData.recruiterEmail || offerData.email || '');
  const source = (offerData.receivedVia || offerData.source || '');

  const normalizedText = (text + ' ' + url + ' ' + email).toLowerCase();
  let score = 0;
  const redFlags = [];
  const positiveSignals = [];

  // Rule 1: Payment & Upfront Fees (Weight: +40) with negation check
  const feeTerms = [
    'registration fee', 'processing fee', 'pay rs', 'security deposit',
    'advance check', 'laptop deposit', 'courier fee', 'zelle', 'upi', 'wire transfer'
  ];
  const hasRawFeeMatch = feeTerms.some(term => normalizedText.includes(term));
  const isNegatedFee =
    /\b(no|not|never|without|zero|neither)\b[\w\s]{0,35}\b(registration fee|processing fee|fee|fees|payment|deposit|security deposit)\b/i.test(normalizedText) ||
    /\b(registration fee|fee|deposit|payment)\b[\w\s]{0,30}\b(not required|never required|is free|waived)\b/i.test(normalizedText);
  const hasFee = hasRawFeeMatch && !isNegatedFee;

  if (hasFee) {
    score += 40;
    redFlags.push({
      icon: 'payments',
      title: 'Advance Payment or Hardware Deposit Demanded',
      severity: 'Critical 10/10',
      description: 'Communication requests upfront financial transfer for registration, laptop courier, training, or background clearance.',
      highlightSnippet: 'Upfront fee / deposit required prior to employment commencement'
    });
  }

  // Rule 2: Personal Webmail instead of Corporate Domain (Weight: +25)
  const hasPersonalEmail =
    /@(gmail|yahoo|hotmail|outlook|zoho|protonmail)\.com/i.test(normalizedText) ||
    /@(gmail|yahoo|hotmail|outlook|zoho|protonmail)\.com/i.test(email);

  if (hasPersonalEmail) {
    score += 25;
    redFlags.push({
      icon: 'alternate_email',
      title: 'Recruiter Uses Free Webmail Address',
      severity: 'Impersonation Risk',
      description: 'Sender contacted you from a public freemail domain instead of an authorized enterprise corporate email server.',
      highlightSnippet: email || 'Public webmail address detected (@gmail / @yahoo)'
    });
  }

  // Rule 3: Unverified Messaging Channels (Weight: +20)
  const hasChatRedirect =
    normalizedText.includes('whatsapp') ||
    normalizedText.includes('telegram') ||
    source === 'whatsapp' ||
    source === 'telegram';

  if (hasChatRedirect) {
    score += 20;
    redFlags.push({
      icon: 'forum',
      title: 'Informal Chat Application Recruiting',
      severity: 'Channel Anomaly',
      description: 'Interview or offer discussions routed via Telegram or WhatsApp to bypass institutional oversight and traceability.',
      highlightSnippet: 'Unmonitored chat conduit (WhatsApp / Telegram)'
    });
  }

  // Rule 4: No Formal Interview / Instant Hiring (Weight: +30)
  const hasInstantJoining =
    normalizedText.includes('no interview') ||
    normalizedText.includes('direct joining') ||
    normalizedText.includes('without interview') ||
    normalizedText.includes('unconditionally selected') ||
    normalizedText.includes('unanimously approved');

  if (hasInstantJoining) {
    score += 30;
    redFlags.push({
      icon: 'speed',
      title: 'Immediate Selection Without Technical Evaluation',
      severity: 'Heuristic Anomaly',
      description: 'Candidate granted full employment without verifiable video, coding assessment, or panel interviews.',
      highlightSnippet: 'Direct selection without formal interview hurdles'
    });
  }

  // Rule 5: Artificial Urgency & Coercive Pressure (Weight: +15)
  const hasUrgency =
    normalizedText.includes('urgent') ||
    normalizedText.includes('immediate hiring') ||
    normalizedText.includes('limited spots') ||
    normalizedText.includes('within 24 hours') ||
    normalizedText.includes('within 12 hours') ||
    normalizedText.includes('respond immediately');

  if (hasUrgency) {
    score += 15;
    redFlags.push({
      icon: 'alarm',
      title: 'High-Pressure Acceptance Deadline',
      severity: 'Coercive Trigger',
      description: 'Imposes tight countdown ultimatums to prevent thorough vetting, company lookup, or parental consultation.',
      highlightSnippet: 'Urgent response demanded'
    });
  }

  // Rule 6: Suspicious Link / Shortener
  const hasSuspiciousUrl =
    url.includes('.xyz') ||
    url.includes('.top') ||
    url.includes('bit.ly') ||
    url.includes('tinyurl') ||
    url.includes('t.me');

  if (hasSuspiciousUrl) {
    score += 15;
    redFlags.push({
      icon: 'link_off',
      title: 'Suspicious Domain TLD or URL Shortener',
      severity: 'Phishing Vector',
      description: 'Recruitment portal is hosted on an untrusted generic TLD (.xyz/.top) or routed through a link obfuscator.',
      highlightSnippet: url
    });
  }

  // Positive signals
  if (!hasFee) {
    positiveSignals.push({
      title: 'Zero Upfront Payment Demanded',
      description: 'Standard enterprise onboarding protocol where no candidate hardware or registration fee is required.'
    });
  }
  if (!hasPersonalEmail && (email.includes('@') || normalizedText.includes('@'))) {
    positiveSignals.push({
      title: 'Corporate Email Structure Present',
      description: 'Sender domain does not use free public webmail providers.'
    });
  }
  if (!hasChatRedirect) {
    positiveSignals.push({
      title: 'Official Recruitment Channel',
      description: 'Offer conveyed through professional email or verified career portal.'
    });
  }

  score = Math.min(score, 100);

  let status = 'HIGHLY_SUSPICIOUS';
  let recommendation = 'Do NOT pay any fees or share personal information. Genuine companies do not ask candidates to pay for jobs. Block the sender and report the offer.';
  let recommendations = [
    'Do not pay any registration fee, security deposit, or hardware charge under any circumstance.',
    'Independently verify the recruiter by contacting the company through verified corporate channels.',
    'Avoid clicking on unverified links, external portals, or anonymous cloud forms.',
    'Contact the company human resources department directly through its official website.'
  ];

  if (score < 40) {
    status = 'LIKELY_GENUINE';
    recommendation = 'This offer appears standard, but always verify the sender’s identity and avoid sharing sensitive financial information.';
    recommendations = [
      'Confirm the offer through the employer’s official recruitment or careers portal.',
      'Never share bank account login credentials or OTPs during onboarding.',
      'Check that communication originates from the company’s official corporate email domain.'
    ];
  } else if (score < 70) {
    status = 'NEEDS_VERIFICATION';
    recommendation = 'Proceed with caution. Independently verify the company’s existence, check their official career page, and do not pay any fees.';
    recommendations = [
      'Do not pay any advance fees or security deposits.',
      'Cross-check the job ID and recruiter profile on official platforms.',
      'Reach out directly to the company via their official website before signing anything.'
    ];
  }

  return {
    score,
    riskScore: score,
    status,
    riskLevel: score >= 70 ? 'HIGH' : score >= 40 ? 'MEDIUM' : 'LOW',
    ruleBasedScore: score,
    aiRiskProbability: null,
    aiClassification: null,
    aiAnalysisAvailable: false,
    reasons: redFlags.map((r) => r.title),
    recommendation,
    recommendations,
    redFlags,
    positiveSignals,
    analysisSummary: `Offline heuristic evaluation completed. Identified ${redFlags.length} threat signal(s).`,
    urlVerification: null,
    recruiterVerification: null,
    source: 'client-heuristic',
    isOffline: true,
    isLocalAnalysis: true,
    analysisMode: 'OFFLINE_HEURISTIC'
  };
}
