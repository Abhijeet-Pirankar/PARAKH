package com.recruitshield.service;

import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.entity.Offer;
import com.recruitshield.entity.RiskReport;
import com.recruitshield.repository.OfferRepository;
import com.recruitshield.repository.RiskReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PARAKH Heuristic Analysis Service.
 * Evaluates job and internship offers across multiple forensic dimensions:
 * 1. Advance fees and financial extortion
 * 2. Sensitive credential & OTP phishing
 * 3. Artificial urgency & coercive deadlines
 * 4. Interview bypass / instant hiring
 * 5. Disproportionate compensation claims
 * 6. Public webmail & disposable recruiter domains
 * 7. Recruiter email vs company domain mismatch
 * 8. Suspicious link shorteners and high-risk TLDs
 * 9. Unmonitored chat channels (WhatsApp, Telegram)
 * 10. Known task-based recruitment scam patterns
 */
@Slf4j
@Service
public class AnalysisService {

    private final OfferRepository offerRepository;
    private final RiskReportRepository riskReportRepository;

    @Autowired
    public AnalysisService(OfferRepository offerRepository, RiskReportRepository riskReportRepository) {
        this.offerRepository = offerRepository;
        this.riskReportRepository = riskReportRepository;
    }

    public AnalysisService() {
        this(null, null);
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern EXTRACT_EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern FREEMAIL_PATTERN = Pattern.compile("@(gmail|yahoo|hotmail|outlook|zoho|rediffmail|protonmail|live|aol)\\.com", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISPOSABLE_EMAIL_PATTERN = Pattern.compile("@(tempmail|throwaway|guerrillamail|mailinator|yopmail|10minutemail)\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_SHORTENER_PATTERN = Pattern.compile("(bit\\.ly|tinyurl\\.com|t\\.me|forms\\.gle|cutt\\.ly|is\\.gd|rb\\.gy|shorturl\\.at)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISKY_TLD_PATTERN = Pattern.compile("https?://[^\\s/]+\\.(xyz|top|tk|ml|ga|cf|gq|work|click|buzz|loan|fit)/?", Pattern.CASE_INSENSITIVE);

    public VerifyResponse analyzeOffer(VerifyRequest request) {
        validateRequest(request);

        String offerText = request.getEffectiveOfferText();
        String companyName = request.getCompanyName() != null ? request.getCompanyName().trim() : "";
        String companyWebsite = request.getCompanyWebsite() != null ? request.getCompanyWebsite().trim() : "";
        String recruiterEmail = request.getRecruiterEmail() != null ? request.getRecruiterEmail().trim() : "";
        String receivedVia = request.getReceivedVia() != null ? request.getReceivedVia().trim() : "";

        String normalizedText = (offerText + " " + companyName + " " + companyWebsite + " " + recruiterEmail + " " + receivedVia).toLowerCase();

        int score = 0;
        List<String> redFlags = new ArrayList<>();
        List<String> positiveSignals = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 1. Advance payment & upfront fees (+40)
        boolean hasPaymentFee = checkPaymentAndFees(normalizedText, redFlags);
        if (hasPaymentFee) {
            score += 40;
            recommendations.add("Do not make any advance payment, security deposit, or equipment transfer under any pretext.");
        }

        // 2. Sensitive Credential & OTP Phishing (+40)
        boolean hasPhishing = checkCredentialPhishing(normalizedText, redFlags);
        if (hasPhishing) {
            score += 40;
            recommendations.add("Never disclose net banking passwords, OTPs, or debit/credit card CVV codes to recruiters.");
        }

        // 3. Interview Bypass / Instant Joining (+30)
        boolean hasInstantJoining = checkInstantJoining(normalizedText, redFlags);
        if (hasInstantJoining) {
            score += 30;
            recommendations.add("Genuine companies conduct verifiable interviews or assessments before extending a binding offer.");
        }

        // 4. Suspicious Recruiter Email Domain (+25)
        boolean hasEmailAnomaly = checkRecruiterEmail(recruiterEmail, normalizedText, redFlags);
        if (hasEmailAnomaly) {
            score += 25;
            recommendations.add("Verify the recruiter's identity by reaching out directly to the company's verified corporate domain.");
        }

        // 5. Recruiter Email vs Company Domain Mismatch (+25)
        boolean hasDomainMismatch = checkDomainMismatch(companyWebsite, companyName, recruiterEmail, normalizedText, redFlags);
        if (hasDomainMismatch) {
            score += 25;
            recommendations.add("Request written communication originating strictly from the company's authorized web domain.");
        }

        // 6. Unrealistic Salary / Task Claims (+25)
        boolean hasUnrealisticComp = checkUnrealisticCompensation(normalizedText, redFlags);
        if (hasUnrealisticComp) {
            score += 25;
            recommendations.add("Be cautious of offers advertising disproportionate earnings for low-skill, part-time tasks.");
        }

        // 7. Suspicious URLs / Shorteners (+20)
        boolean hasSuspiciousUrl = checkSuspiciousUrls(companyWebsite, normalizedText, redFlags);
        if (hasSuspiciousUrl) {
            score += 20;
            recommendations.add("Avoid clicking shortened links or submitting personal credentials on unverified third-party forms.");
        }

        // 8. Informal / Unmonitored Messaging Channel (+20)
        boolean hasInformalChannel = checkInformalChannel(receivedVia, normalizedText, redFlags);
        if (hasInformalChannel) {
            score += 20;
            recommendations.add("Legitimate employers use formal enterprise communication rather than unmonitored chat apps.");
        }

        // 9. Coercive Urgency & Pressure (+15)
        boolean hasUrgency = checkSuspiciousUrgency(normalizedText, redFlags);
        if (hasUrgency) {
            score += 15;
            recommendations.add("Do not let artificial deadline pressure prevent you from conducting thorough background checks.");
        }

        // 10. Task Scam Language (+20)
        boolean hasTaskScamLanguage = checkTaskScamLanguage(normalizedText, redFlags);
        if (hasTaskScamLanguage) {
            score += 20;
            recommendations.add("Decline task-based or review-based commissions that require initial financial deposits.");
        }

        // Determine Positive Signals
        if (!hasPaymentFee) {
            positiveSignals.add("No upfront payment, registration fee, or equipment deposit demanded");
        }
        if (!companyWebsite.isEmpty() && isStandardDomain(companyWebsite)) {
            positiveSignals.add("Company website provided with standard web domain");
        }
        if (!recruiterEmail.isEmpty() && !FREEMAIL_PATTERN.matcher(recruiterEmail).find() && !DISPOSABLE_EMAIL_PATTERN.matcher(recruiterEmail).find()) {
            positiveSignals.add("Recruiter contacted from an enterprise corporate email domain");
        }
        if (normalizedText.contains("interview round") || normalizedText.contains("technical interview") || normalizedText.contains("coding assessment") || normalizedText.contains("panel interview")) {
            positiveSignals.add("Structured multi-round technical evaluation process mentioned");
        }
        if (receivedVia.equalsIgnoreCase("LinkedIn") || receivedVia.equalsIgnoreCase("Email") || receivedVia.equalsIgnoreCase("Company Portal")) {
            positiveSignals.add("Offer originated through standard professional recruitment avenue");
        }

        // Cap score at 100
        int riskScore = Math.min(100, Math.max(0, score));

        String status = determineStatus(riskScore);
        String riskLevel = determineRiskLevel(riskScore);
        String analysisSummary = determineSummary(riskScore, redFlags.size());

        // Default safety recommendations if none triggered
        if (recommendations.isEmpty()) {
            recommendations.add("Verify the job opening on the company's official career portal.");
            recommendations.add("Ensure all official correspondence originates from the company's verified email domain.");
            recommendations.add("Never share confidential banking credentials or OTPs during onboarding.");
        }

        // Persist Offer and RiskReport to MySQL if repositories are configured
        if (offerRepository != null && riskReportRepository != null) {
            try {
                Offer offer = Offer.builder()
                        .offerText(offerText)
                        .companyName(companyName.isEmpty() ? null : companyName)
                        .companyWebsite(companyWebsite.isEmpty() ? null : companyWebsite)
                        .recruiterEmail(recruiterEmail.isEmpty() ? null : recruiterEmail)
                        .receivedVia(receivedVia.isEmpty() ? null : receivedVia)
                        .createdAt(LocalDateTime.now())
                        .build();

                Offer savedOffer = offerRepository.save(offer);

                RiskReport riskReport = RiskReport.builder()
                        .offer(savedOffer)
                        .riskScore(riskScore)
                        .status(status)
                        .riskLevel(riskLevel)
                        .summary(analysisSummary)
                        .redFlags(new ArrayList<>(redFlags.isEmpty() ? List.of("No obvious red flags detected in this text.") : redFlags))
                        .positiveSignals(new ArrayList<>(positiveSignals))
                        .recommendations(new ArrayList<>(recommendations))
                        .createdAt(LocalDateTime.now())
                        .build();

                riskReportRepository.save(riskReport);
                savedOffer.setRiskReport(riskReport);
                log.info("Persisted Offer ID {} and RiskReport ID {} to database", savedOffer.getId(), riskReport.getId());
            } catch (Exception ex) {
                log.error("Failed to persist offer analysis to database: {}", ex.getMessage(), ex);
            }
        }

        return VerifyResponse.builder()
                .riskScore(riskScore)
                .score(riskScore)
                .status(status)
                .riskLevel(riskLevel)
                .redFlags(redFlags.isEmpty() ? List.of("No obvious red flags detected in this text.") : redFlags)
                .reasons(redFlags.isEmpty() ? List.of("No obvious red flags detected in this text.") : redFlags)
                .positiveSignals(positiveSignals)
                .recommendations(recommendations)
                .recommendation(recommendations.get(0))
                .analysisSummary(analysisSummary)
                .build();
    }

    public void validateRequest(VerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null.");
        }
        String text = request.getEffectiveOfferText();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Offer text cannot be empty or blank.");
        }
        if (request.getRecruiterEmail() != null && !request.getRecruiterEmail().trim().isEmpty()) {
            String email = request.getRecruiterEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid recruiter email format: " + email);
            }
        }
    }

    private boolean checkPaymentAndFees(String text, List<String> redFlags) {
        boolean match = text.contains("registration fee") ||
                text.contains("processing fee") ||
                text.contains("application fee") ||
                text.contains("security deposit") ||
                text.contains("refundable deposit") ||
                text.contains("training fee") ||
                text.contains("laptop fee") ||
                text.contains("courier fee") ||
                text.contains("material fee") ||
                text.contains("kit fee") ||
                text.contains("advance payment") ||
                text.contains("pay advance") ||
                text.contains("initial payment") ||
                text.contains("pay rs") ||
                text.contains("pay inr") ||
                text.contains("transfer amount") ||
                text.contains("deposit amount") ||
                text.contains("upi id") ||
                text.contains("gpay") ||
                text.contains("phonepe") ||
                text.contains("wire transfer") ||
                text.contains("crypto") ||
                text.contains("usdt");

        if (match) {
            redFlags.add("Advance payment, registration fee, or security deposit demanded");
        }
        return match;
    }

    private boolean checkCredentialPhishing(String text, List<String> redFlags) {
        boolean match = (text.contains("otp") && (text.contains("share") || text.contains("send") || text.contains("verify"))) ||
                text.contains("one-time password") ||
                text.contains("net banking password") ||
                text.contains("bank account password") ||
                text.contains("atm pin") ||
                text.contains("cvv") ||
                text.contains("debit card pin") ||
                text.contains("share your password");

        if (match) {
            redFlags.add("Requests sensitive financial credentials, OTP, or passwords");
        }
        return match;
    }

    private boolean checkInstantJoining(String text, List<String> redFlags) {
        boolean match = text.contains("no interview") ||
                text.contains("direct joining") ||
                text.contains("without interview") ||
                text.contains("direct selection") ||
                text.contains("unconditionally selected") ||
                text.contains("spot selection");

        if (match) {
            redFlags.add("Promises direct employment or instant joining without a proper interview process");
        }
        return match;
    }

    private boolean checkRecruiterEmail(String email, String text, List<String> redFlags) {
        boolean foundFreemail = false;
        boolean foundDisposable = false;

        if (!email.isEmpty()) {
            if (FREEMAIL_PATTERN.matcher(email).find()) {
                foundFreemail = true;
            }
            if (DISPOSABLE_EMAIL_PATTERN.matcher(email).find()) {
                foundDisposable = true;
            }
        } else {
            Matcher matcher = EXTRACT_EMAIL_PATTERN.matcher(text);
            while (matcher.find()) {
                String candidate = matcher.group();
                if (FREEMAIL_PATTERN.matcher(candidate).find()) {
                    foundFreemail = true;
                }
                if (DISPOSABLE_EMAIL_PATTERN.matcher(candidate).find()) {
                    foundDisposable = true;
                }
            }
        }

        if (foundDisposable) {
            redFlags.add("Recruiter uses a disposable or anonymous email address");
            return true;
        } else if (foundFreemail) {
            redFlags.add("Recruiter uses a public freemail domain (@gmail/@yahoo) instead of a corporate domain");
            return true;
        }
        return false;
    }

    private boolean checkDomainMismatch(String companyWebsite, String companyName, String recruiterEmail, String text, List<String> redFlags) {
        if (recruiterEmail.isEmpty() || companyWebsite.isEmpty()) {
            return false;
        }

        String emailDomain = extractDomainFromEmail(recruiterEmail);
        String siteDomain = extractDomainFromUrl(companyWebsite);

        if (!emailDomain.isEmpty() && !siteDomain.isEmpty()) {
            if (!emailDomain.equalsIgnoreCase(siteDomain) && !siteDomain.endsWith("." + emailDomain) && !emailDomain.endsWith("." + siteDomain)) {
                redFlags.add("Recruiter email domain (" + emailDomain + ") does not match official company domain (" + siteDomain + ")");
                return true;
            }
        }
        return false;
    }

    private boolean checkUnrealisticCompensation(String text, List<String> redFlags) {
        boolean match = text.contains("earn 50000 per day") ||
                text.contains("earn 10000 daily") ||
                text.contains("daily payout") ||
                text.contains("50k per day") ||
                text.contains("guaranteed income without skills") ||
                text.contains("high income minimal work") ||
                text.contains("earn 1 lakh per month part time");

        if (match) {
            redFlags.add("Promises unrealistic salary or daily payouts disproportionate to qualifications");
        }
        return match;
    }

    private boolean checkSuspiciousUrls(String website, String text, List<String> redFlags) {
        boolean match = false;
        if (!website.isEmpty()) {
            if (URL_SHORTENER_PATTERN.matcher(website).find() || RISKY_TLD_PATTERN.matcher(website).find()) {
                match = true;
            }
        }
        if (URL_SHORTENER_PATTERN.matcher(text).find() || RISKY_TLD_PATTERN.matcher(text).find()) {
            match = true;
        }

        if (match) {
            redFlags.add("Contains suspicious shortened URL, anonymous cloud form, or untrusted domain TLD");
        }
        return match;
    }

    private boolean checkInformalChannel(String receivedVia, String text, List<String> redFlags) {
        boolean match = (receivedVia != null && (receivedVia.equalsIgnoreCase("WhatsApp") || receivedVia.equalsIgnoreCase("Telegram"))) ||
                text.contains("contact on whatsapp") ||
                text.contains("join telegram") ||
                text.contains("telegram group") ||
                text.contains("whatsapp hr");

        if (match) {
            redFlags.add("Encourages recruitment communication via informal, unverified chat apps (WhatsApp/Telegram)");
        }
        return match;
    }

    private boolean checkSuspiciousUrgency(String text, List<String> redFlags) {
        boolean match = text.contains("urgent") ||
                text.contains("immediate hiring") ||
                text.contains("limited spots") ||
                text.contains("within 24 hours") ||
                text.contains("within 12 hours") ||
                text.contains("today only") ||
                text.contains("respond immediately") ||
                text.contains("final notice");

        if (match) {
            redFlags.add("Creates artificial urgency or coercive deadline pressure");
        }
        return match;
    }

    private boolean checkTaskScamLanguage(String text, List<String> redFlags) {
        boolean match = text.contains("like and subscribe") ||
                text.contains("youtube video like") ||
                text.contains("hotel review task") ||
                text.contains("movie rating task") ||
                text.contains("commission on investment") ||
                text.contains("cryptocurrency task");

        if (match) {
            redFlags.add("Exhibits characteristics of task-based commission or prepaid investment scams");
        }
        return match;
    }

    private String extractDomainFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex != -1 && atIndex < email.length() - 1) {
            return email.substring(atIndex + 1).toLowerCase().trim();
        }
        return "";
    }

    private String extractDomainFromUrl(String url) {
        try {
            String clean = url.trim();
            if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                clean = "https://" + clean;
            }
            URI uri = new URI(clean);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4).toLowerCase() : host.toLowerCase();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean isStandardDomain(String url) {
        String domain = extractDomainFromUrl(url);
        return !domain.isEmpty() && !URL_SHORTENER_PATTERN.matcher(domain).find() && !domain.endsWith(".xyz") && !domain.endsWith(".top");
    }

    private String determineStatus(int score) {
        if (score < 30) {
            return "LIKELY_GENUINE";
        } else if (score < 60) {
            return "NEEDS_VERIFICATION";
        } else {
            return "HIGHLY_SUSPICIOUS";
        }
    }

    private String determineRiskLevel(int score) {
        if (score < 30) {
            return "LOW";
        } else if (score < 60) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    private String determineSummary(int score, int flagCount) {
        if (score < 30) {
            return "The offer exhibits standard recruitment patterns with no major red flags detected.";
        } else if (score < 60) {
            return "The offer contains " + flagCount + " potential indicator(s) requiring candidate verification before proceeding.";
        } else {
            return "The offer contains multiple severe indicators (" + flagCount + " red flags) that require immediate caution.";
        }
    }
}
