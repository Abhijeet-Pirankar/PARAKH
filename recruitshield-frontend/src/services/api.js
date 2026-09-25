/**
 * PARAKH Backend API Client
 * Connects frontend verification workbench to Spring Boot REST endpoints.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Sends offer details to the Spring Boot rule-based risk analysis engine.
 * @param {Object} offerData
 * @param {string} offerData.offerText - Primary offer narrative or communication text
 * @param {string} [offerData.companyName] - Stated company/employer name
 * @param {string} [offerData.companyWebsite] - Company website or career portal URL
 * @param {string} [offerData.recruiterEmail] - Sender / recruiter email
 * @param {string} [offerData.receivedVia] - Communication channel (WhatsApp, Telegram, etc.)
 * @returns {Promise<Object>} Verification risk report
 */
export async function analyzeOffer(offerData) {
  const payload = {
    offerText: offerData.offerText ? offerData.offerText.trim() : '',
    companyName: offerData.companyName ? offerData.companyName.trim() : '',
    companyWebsite: offerData.companyWebsite ? offerData.companyWebsite.trim() : '',
    recruiterEmail: offerData.recruiterEmail ? offerData.recruiterEmail.trim() : '',
    receivedVia: offerData.receivedVia ? offerData.receivedVia.trim() : ''
  };

  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/analyze-offer`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
  } catch (err) {
    // Network / server connection failure
    const connectionError = new Error('BACKEND_UNAVAILABLE');
    connectionError.code = 'BACKEND_UNAVAILABLE';
    connectionError.originalError = err;
    throw connectionError;
  }

  if (!response.ok) {
    let errorDetails = null;
    try {
      errorDetails = await response.json();
    } catch (_) {
      // response wasn't JSON
    }

    const message = errorDetails?.message || `Backend returned error (HTTP ${response.status})`;
    const apiError = new Error(message);
    apiError.status = response.status;
    apiError.details = errorDetails;
    throw apiError;
  }

  return await response.json();
}
