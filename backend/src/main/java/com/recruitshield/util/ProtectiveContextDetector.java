package com.recruitshield.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reusable, Transparent Negation and Protective Context Detection Utility.
 * Distinguishes between affirmative suspicious requests/actions, legitimate
 * warnings/protective statements, and explicit negations.
 *
 * SAFETY GUARANTEE:
 * Operates purely offline via deterministic regex and clause analysis.
 * Does not require external heavyweight NLP dependencies.
 */
public final class ProtectiveContextDetector {

    private ProtectiveContextDetector() {
        // Utility class
    }

    // Splits text into semantic clauses using sentence punctuation or adversative conjunctions
    private static final Pattern CLAUSE_DELIMITER = Pattern.compile(
            "[\\.\\!\\?\\;\\n\\r]+|\\b(?:but|however|whereas|although|nonetheless)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Preceding guard patterns: Explicit negations and protective warning verbs
    private static final Pattern NEGATION_PREFIX = Pattern.compile(
            "\\b(?:no|not|never|neither|nor|without|zero|free\\s+of|free\\s+from|exempt\\s+from)\\b"
            + "|\\b(?:do|does|did|will|would|can|could|shall|should)\\s+not\\b"
            + "|\\b(?:don'?t|doesn'?t|didn'?t|won'?t|wouldn'?t|can'?t|cannot)\\b"
            + "|\\b(?:never\\s+(?:ask|demand|require|request|charge|accept|collect|pay|transfer|send|share|disclose|give|provide))\\b"
            + "|\\b(?:do\\s+not|don'?t)\\s+(?:pay|transfer|send|share|disclose|give|provide|deposit|submit)\\b"
            + "|\\b(?:does\\s+not|doesn'?t|will\\s+not|won'?t)\\s+(?:ask|demand|require|request|charge|collect)\\b"
            + "|\\b(?:caution|warning|beware|scam\\s+alert|fraud\\s+alert|disclaimer)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Following guard patterns: Passive negation predicates and protective qualifiers
    private static final Pattern NEGATION_SUFFIX = Pattern.compile(
            "\\b(?:is|are|was|were)?\\s*(?:not|never)\\s+(?:required|demanded|asked|needed|charged|accepted|collected)\\b"
            + "|\\b(?:is|are|was|were)?\\s*(?:completely|totally)?\\s*(?:waived|exempted|free(?:\\s+of\\s+charge)?)\\b"
            + "|\\b(?:not\\s+required|never\\s+required)\\b"
            + "|\\b(?:is|are|was|were)?\\s*(?:optional|not\\s+mandatory)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Payment and Fee Demands
    private static final List<Pattern> PAYMENT_FEE_PATTERNS = Collections.unmodifiableList(Arrays.asList(
            Pattern.compile("\\b(?:registration|processing|application|training|laptop|courier|material|kit|security|refundable|advance|initial)\\s+(?:fee|fees|deposit|payment|amount|check)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:pay\\s+advance|pay\\s+rs\\.?|pay\\s+inr|transfer\\s+amount|deposit\\s+amount)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:upi\\s+id|gpay|phonepe|wire\\s+transfer|usdt|cryptocurrency)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpay\\s+(?:a\\s+|the\\s+|any\\s+|some\\s+)?(?:fee|fees|deposit|amount|money|charge)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpayment\\s+is\\s+required\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpay\\s+(?:rs|inr|\\$|₹)?\\s*\\d+\\b", Pattern.CASE_INSENSITIVE)
    ));

    // Credential and Phishing Demands
    private static final List<Pattern> CREDENTIAL_PHISHING_PATTERNS = Collections.unmodifiableList(Arrays.asList(
            Pattern.compile("\\b(?:share|send|verify|provide|enter|submit|give)\\s+(?:your\\s+)?(?:otp|one-time\\s+password|cvv|pin|password|bank\\s+details|banking\\s+password)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:otp|one-time\\s+password)\\s+(?:verification|is\\s+required|needed|must\\s+be\\s+shared)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:net\\s+banking\\s+password|bank\\s+account\\s+password|atm\\s+pin|debit\\s+card\\s+pin|cvv|share\\s+your\\s+password)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:provide|send|share|submit|enter)\\s+(?:your\\s+)?(?:bank\\s+details|banking\\s+credentials|account\\s+password)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:ask\\s+for\\s+your\\s+password)\\b", Pattern.CASE_INSENSITIVE)
    ));

    /**
     * Checks if the text contains an un-negated payment or fee demand.
     */
    public static boolean containsUnnegatedPaymentFeeDemand(String text) {
        return containsUnnegatedMatch(text, PAYMENT_FEE_PATTERNS);
    }

    /**
     * Checks if the text contains an un-negated credential or password phishing demand.
     */
    public static boolean containsUnnegatedCredentialPhishing(String text) {
        return containsUnnegatedMatch(text, CREDENTIAL_PHISHING_PATTERNS);
    }

    /**
     * Inspects clauses in text and determines if any match occurs in an affirmative (non-negated, non-protective) context.
     *
     * @param text Raw or normalized text
     * @param patterns Suspicious target patterns
     * @return true if an un-negated suspicious match is detected; false if clean or fully negated/protective
     */
    public static boolean containsUnnegatedMatch(String text, List<Pattern> patterns) {
        if (text == null || text.trim().isEmpty() || patterns == null || patterns.isEmpty()) {
            return false;
        }

        String[] clauses = CLAUSE_DELIMITER.split(text);
        for (String rawClause : clauses) {
            String clause = rawClause.trim();
            if (clause.isEmpty()) {
                continue;
            }

            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(clause);
                while (matcher.find()) {
                    if (!isMatchNegatedOrProtective(clause, matcher.start(), matcher.end())) {
                        return true; // Found affirmative suspicious demand
                    }
                }
            }
        }

        return false;
    }

    /**
     * Evaluates context window around a specific match within a clause.
     */
    private static boolean isMatchNegatedOrProtective(String clause, int matchStart, int matchEnd) {
        String preText = clause.substring(0, matchStart).trim();
        String postText = clause.substring(matchEnd).trim();

        // Examine preceding tokens (up to ~8 words / 60 characters)
        String[] preWords = preText.split("\\s+");
        int preWordCount = Math.min(preWords.length, 8);
        StringBuilder preWindowBuilder = new StringBuilder();
        for (int i = preWords.length - preWordCount; i < preWords.length; i++) {
            if (i >= 0 && !preWords[i].isEmpty()) {
                if (preWindowBuilder.length() > 0) preWindowBuilder.append(" ");
                preWindowBuilder.append(preWords[i]);
            }
        }
        String preWindow = preWindowBuilder.toString();

        // Examine following tokens (up to ~8 words / 60 characters)
        String[] postWords = postText.split("\\s+");
        int postWordCount = Math.min(postWords.length, 8);
        StringBuilder postWindowBuilder = new StringBuilder();
        for (int i = 0; i < postWordCount && i < postWords.length; i++) {
            if (!postWords[i].isEmpty()) {
                if (postWindowBuilder.length() > 0) postWindowBuilder.append(" ");
                postWindowBuilder.append(postWords[i]);
            }
        }
        String postWindow = postWindowBuilder.toString();

        boolean isNegatedPre = NEGATION_PREFIX.matcher(preWindow).find();
        boolean isNegatedPost = NEGATION_SUFFIX.matcher(postWindow).find();

        return isNegatedPre || isNegatedPost;
    }
}
