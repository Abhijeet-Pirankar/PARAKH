/**
 * PARAKH Forensic Verification Service
 * Connects to Spring Boot API (/api/verify) on http://localhost:8080
 * with graceful fallback to heuristic analysis engine if backend is unreachable.
 */

const BACKEND_URL = 'http://localhost:8080/api/verify';

/**
 * Run client-side heuristic fallback mirroring Spring Boot AnalysisService
 */
export function analyzeOfferLocally({ text = '', url = '', email = '', source = '' }) {
  const normalizedText = (text + ' ' + url + ' ' + email).toLowerCase();
  let score = 0;
  const redFlags = [];
  const positiveSignals = [];

  // Rule 1: Payment & Upfront Fees (Weight: +40)
  const hasFee =
    normalizedText.includes('registration fee') ||
    normalizedText.includes('processing fee') ||
    normalizedText.includes('pay rs') ||
    normalizedText.includes('deposit') ||
    normalizedText.includes('security deposit') ||
    normalizedText.includes('advance check') ||
    normalizedText.includes('laptop deposit') ||
    normalizedText.includes('courier fee') ||
    normalizedText.includes('zelle') ||
    normalizedText.includes('upi') ||
    normalizedText.includes('wire transfer');

  if (hasFee) {
    score += 40;
    redFlags.push({
      icon: 'credit_card_off',
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
      icon: 'domain_verification',
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

  // Check positive signals
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

  // Cap score at 100
  score = Math.min(score, 100);

  // Status & Recommendations
  let status = 'Highly Suspicious';
  let recommendation = 'Do NOT pay any fees or share personal information. Genuine companies do not ask candidates to pay for jobs. Block the sender and report the offer.';
  let recommendations = [
    'Do not pay any registration fee, security deposit, or hardware charge under any circumstance.',
    'Independently verify the recruiter by contacting the company through verified corporate channels.',
    'Avoid clicking on unverified links, external portals, or anonymous cloud forms.',
    'Contact the company human resources department directly through its official website.'
  ];

  if (score < 40) {
    status = 'Likely Genuine';
    recommendation = 'This offer appears standard, but always verify the sender’s identity and avoid sharing sensitive financial information.';
    recommendations = [
      'Confirm the offer through the employer’s official recruitment or careers portal.',
      'Never share bank account login credentials or OTPs during onboarding.',
      'Check that communication originates from the company’s official corporate email domain.'
    ];
  } else if (score < 70) {
    status = 'Needs Verification';
    recommendation = 'Proceed with caution. Independently verify the company’s existence, check their official career page, and do not pay any fees.';
    recommendations = [
      'Do not pay any advance fees or security deposits.',
      'Cross-check the job ID and recruiter profile on official platforms.',
      'Reach out directly to the company via their official website before signing anything.'
    ];
  }

  return {
    score,
    status,
    reasons: redFlags.map((r) => r.title),
    recommendation,
    recommendations,
    redFlags,
    positiveSignals,
    source: 'client-heuristic'
  };
}

/**
 * Main verification function
 * 1. Calls Spring Boot backend POST /api/verify
 * 2. Merges with client-side forensic enrichment
 * 3. Falls back gracefully if backend is offline
 */
export async function verifyOffer({ text, url = '', email = '', source = '' }) {
  const payloadText = [
    text || '',
    url ? `Website: ${url}` : '',
    email ? `Recruiter Email: ${email}` : '',
    source ? `Channel: ${source}` : ''
  ]
    .filter(Boolean)
    .join('\n');

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000); // 4s timeout

    const response = await fetch(BACKEND_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ text: payloadText }),
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (response.ok) {
      const data = await response.json();
      
      // Also generate rich UI details aligned with backend score
      const localDetails = analyzeOfferLocally({ text, url, email, source });
      
      return {
        score: data.score !== undefined ? data.score : localDetails.score,
        status: data.status || localDetails.status,
        reasons: data.reasons && data.reasons.length > 0 ? data.reasons : localDetails.reasons,
        recommendation: data.recommendation || localDetails.recommendation,
        recommendations: localDetails.recommendations,
        redFlags: localDetails.redFlags,
        positiveSignals: localDetails.positiveSignals,
        source: 'backend-api'
      };
    } else {
      console.warn('Backend returned non-200 status, using heuristic fallback:', response.status);
      return analyzeOfferLocally({ text, url, email, source });
    }
  } catch (err) {
    console.info('Backend unreachable, conducting client-side heuristic evaluation:', err.message);
    return analyzeOfferLocally({ text, url, email, source });
  }
}
