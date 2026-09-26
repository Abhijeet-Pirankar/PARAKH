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
    private final UrlVerificationService urlVerificationService;
    private final RecruiterVerificationService recruiterVerificationService;

    @Autowired
    public AnalysisService(
            OfferRepository offerRepository,
            RiskReportRepository riskReportRepository,
            UrlVerificationService urlVerificationService,
            RecruiterVerificationService recruiterVerificationService) {
        this.offerRepository = offerRepository;
        this.riskReportRepository = riskReportRepository;
        this.urlVerificationService = urlVerificationService != null ? urlVerificationService : new UrlVerificationService();
        this.recruiterVerificationService = recruiterVerificationService != null ? recruiterVerificationService : new RecruiterVerificationService();
    }

    public AnalysisService(OfferRepository offerRepository, RiskReportRepository riskReportRepository) {
        this(offerRepository, riskReportRepository, new UrlVerificationService(), new RecruiterVerificationService());
    }

    public AnalysisService() {
        this(null, null, new UrlVerificationService(), new RecruiterVerificationService());
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern URL_SHORTENER_PATTERN = Pattern.compile("(bit\\.ly|tinyurl\\.com|t\\.me|forms\\.gle|cutt\\.ly|is\\.gd|rb\\.gy|shorturl\\.at)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISKY_TLD_PATTERN = Pattern.compile("https?://[^\\s/]+\\.(xyz|top|tk|ml|ga|cf|gq|work|click|buzz|loan|fit)/?", Pattern.CASE_INSENSITIVE);

    public VerifyResponse analyzeOffer(VerifyRequest request) {
        validateRequest(request);

        String offerText = request.getEffectiveOfferText();
        String companyName = request.getCompanyName() != null ? request.getCompanyName().trim() : "";
        String companyWebsite = request.getCompanyWebsite() != null ? request.getCompanyWebsite().trim() : "";
        String recruiterEmail = request.getRecruiterEmail() != null ? request.getRecruiterEmail().trim() : "";
        String receivedVia = request.getReceivedVia() != null ? request.getReceivedVia().trim() : "";

        String normalizedText = (offerText + " " + companyName + " " + companyWebsite + " " + recruiterEmail + " " + receivedVia).toLowerCase(java.util.Locale.ROOT);

        int score = 0;
        List<String> redFlags = new ArrayList<>();
        List<String> positiveSignals = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 0. Static URL and Recruiter Forensic Analysis
        com.recruitshield.dto.UrlVerificationResult urlResult = urlVerificationService.verifyUrl(companyWebsite);
        com.recruitshield.dto.RecruiterVerificationResult recruiterResult = recruiterVerificationService.verifyRecruiter(
                recruiterEmail, companyWebsite, receivedVia, offerText);

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

        // 4. Recruiter Email Verification Findings
        if (recruiterResult.isDisposableEmail()) {
            score += 30;
            redFlags.add("Recruiter uses a disposable or anonymous email address");
            recommendations.add("Verify the recruiter's identity by reaching out directly to the company's verified corporate domain.");
        } else if (recruiterResult.isPublicFreemail()) {
            score += 25;
            redFlags.add("Recruiter uses a public freemail domain (@gmail/@yahoo) instead of a corporate domain");
            recommendations.add("Verify the recruiter's identity by reaching out directly to the company's verified corporate domain.");
        }
        if (recruiterResult.isSuspiciousPattern()) {
            score += 15;
            redFlags.add("Recruiter email address exhibits automated or randomized naming patterns");
        }

        // 5. Recruiter Email vs Company Domain Mismatch (+25)
        if (Boolean.FALSE.equals(recruiterResult.getDomainMatch())) {
            score += 25;
            String emailDomain = recruiterResult.getEmailDomain() != null ? recruiterResult.getEmailDomain() : "";
            String siteDomain = recruiterResult.getCompanyDomain() != null ? recruiterResult.getCompanyDomain() : "";
            redFlags.add("Recruiter email domain (" + emailDomain + ") does not match official company domain (" + siteDomain + ")");
            recommendations.add("Request written communication originating strictly from the company's authorized web domain.");
        }

        // 6. Unrealistic Salary / Task Claims (+25)
        boolean hasUnrealisticComp = checkUnrealisticCompensation(normalizedText, redFlags);
        if (hasUnrealisticComp) {
            score += 25;
            recommendations.add("Be cautious of offers advertising disproportionate earnings for low-skill, part-time tasks.");
        }

        // 7. URL Verification Findings
        boolean textHasSuspiciousUrl = URL_SHORTENER_PATTERN.matcher(normalizedText).find() || RISKY_TLD_PATTERN.matcher(normalizedText).find();
        if (urlResult.isUrlShortener() || urlResult.isRiskyTld() || textHasSuspiciousUrl) {
            score += 20;
            redFlags.add("Contains suspicious shortened URL, anonymous cloud form, or untrusted domain TLD");
            recommendations.add("Avoid clicking shortened links or submitting personal credentials on unverified third-party forms.");
        }
        if (urlResult.isIpAddress()) {
            score += 20;
            redFlags.add("Company website uses a raw IP address instead of a domain name");
            recommendations.add("Avoid navigating to raw IP addresses or entering credentials on unverified hosts.");
        }
        if (urlResult.isPrivateOrLocal()) {
            score += 25;
            redFlags.add("Company website points to a local or private network address");
            recommendations.add("Do not interact with websites pointing to private or internal network addresses.");
        }
        if (urlResult.isProvided() && !urlResult.isHttps() && urlResult.isValid()) {
            score += 10;
            redFlags.add("Company website uses insecure HTTP instead of encrypted HTTPS");
            recommendations.add("Exercise caution when interacting with unencrypted websites that do not support HTTPS.");
        }
        if (urlResult.isExcessiveSubdomains()) {
            score += 10;
            redFlags.add("Company website contains an unusually high number of subdomain levels");
        }
        if (urlResult.isSuspiciousKeywords()) {
            score += 15;
            redFlags.add("Company website domain includes keywords commonly associated with credential theft or phishing");
        }

        // 8. Informal / Unmonitored Messaging Channel (+20)
        if (recruiterResult.isInformalChannel()) {
            score += 20;
            redFlags.add("Encourages recruitment communication via informal, unverified chat apps (WhatsApp/Telegram)");
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

        // Determine Positive Authenticity Signals
        if (!hasPaymentFee) {
            positiveSignals.add("No upfront payment, registration fee, or equipment deposit demanded");
        }
        if (urlResult.isProvided() && urlResult.isHttps() && urlResult.isValid()) {
            positiveSignals.add("Company website uses HTTPS");
        }
        if (urlResult.isProvided() && urlResult.isValid() && !urlResult.isIpAddress() && !urlResult.isUrlShortener() && !urlResult.isRiskyTld()) {
            positiveSignals.add("Company website provided with standard web domain");
        }
        if (Boolean.TRUE.equals(recruiterResult.getDomainMatch())) {
            positiveSignals.add("Recruiter email domain matches the provided company website domain");
        }
        if (recruiterResult.isEmailProvided() && !recruiterResult.isPublicFreemail() && !recruiterResult.isDisposableEmail() && recruiterResult.isEmailValid()) {
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
                .urlVerification(urlResult)
                .recruiterVerification(recruiterResult)
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
