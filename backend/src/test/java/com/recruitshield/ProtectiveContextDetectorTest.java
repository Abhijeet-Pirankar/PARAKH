package com.recruitshield;

import com.recruitshield.util.ProtectiveContextDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProtectiveContextDetector.
 * Validates explicit negation, protective security warnings, and affirmative scam demands.
 */
class ProtectiveContextDetectorTest {

    @ParameterizedTest(name = "Safe Fee Statement: \"{0}\"")
    @ValueSource(strings = {
            "There is no registration fee or payment required.",
            "No advance payment is required.",
            "No security deposit is required.",
            "We never ask candidates to pay any fee.",
            "No payment is required at any stage.",
            "Do not pay any registration fee or security deposit under any circumstances.",
            "Without any registration fee or payment required.",
            "Zero registration fee or upfront payment needed.",
            "Our company does not require any initial payment or courier fee.",
            "All onboarding is completely free of charge and no deposit is required."
    })
    @DisplayName("Legitimate negated fee phrases must NOT trigger payment fee demand")
    void testNegatedPaymentFeeStatements(String text) {
        assertFalse(ProtectiveContextDetector.containsUnnegatedPaymentFeeDemand(text),
                "Expected safe/negated text not to trigger payment fee demand: " + text);
    }

    @ParameterizedTest(name = "Safe Credential Statement: \"{0}\"")
    @ValueSource(strings = {
            "Never share your OTP with anyone.",
            "We will never ask for your password.",
            "Do not share your bank details.",
            "Never disclose your net banking password or ATM pin.",
            "Recruiters will never ask candidates to share their OTP or passwords.",
            "Caution: Do not submit your banking credentials on unverified third-party websites."
    })
    @DisplayName("Legitimate security warnings must NOT trigger credential phishing demand")
    void testNegatedCredentialPhishingStatements(String text) {
        assertFalse(ProtectiveContextDetector.containsUnnegatedCredentialPhishing(text),
                "Expected protective warning not to trigger credential phishing: " + text);
    }

    @ParameterizedTest(name = "Scam Fee Statement: \"{0}\"")
    @ValueSource(strings = {
            "Pay a registration fee of Rs. 5000.",
            "Send an advance payment to confirm your internship.",
            "Pay the security deposit before joining.",
            "Pay Rs. 2000 for training.",
            "Direct selection without interview. Pay Rs 2500 registration fee.",
            "You must pay 1500 INR processing fee to book your slot.",
            "Send Rs 1000 laptop deposit to our UPI id.",
            "Initial payment of Rs 3000 required before onboarding."
    })
    @DisplayName("Affirmative scam fee demands MUST trigger payment fee demand")
    void testScamPaymentFeeStatements(String text) {
        assertTrue(ProtectiveContextDetector.containsUnnegatedPaymentFeeDemand(text),
                "Expected scam demand to be detected: " + text);
    }

    @ParameterizedTest(name = "Scam Credential Statement: \"{0}\"")
    @ValueSource(strings = {
            "Share your OTP to complete verification.",
            "Send your password for account verification.",
            "Provide your bank details to receive the salary.",
            "Submit your net banking password to confirm your profile.",
            "Please enter your CVV to verify your candidate bank account."
    })
    @DisplayName("Affirmative credential phishing demands MUST trigger credential phishing demand")
    void testScamCredentialPhishingStatements(String text) {
        assertTrue(ProtectiveContextDetector.containsUnnegatedCredentialPhishing(text),
                "Expected credential phishing demand to be detected: " + text);
    }

    @Test
    @DisplayName("Mixed clauses: Scam demand following a disclaimer MUST still be detected")
    void testMixedDisclaimerAndScamDemand() {
        String mixed1 = "We never ask for money. However, please pay a registration fee of Rs. 5000 to proceed.";
        assertTrue(ProtectiveContextDetector.containsUnnegatedPaymentFeeDemand(mixed1),
                "Scam demand in second clause must be detected despite disclaimer in first clause");

        String mixed2 = "Never share your OTP with anyone, but provide your bank details to receive the salary.";
        assertTrue(ProtectiveContextDetector.containsUnnegatedCredentialPhishing(mixed2),
                "Phishing demand in second clause must be detected despite protective warning in first clause");
    }

    @Test
    @DisplayName("Null or empty input handled safely without exceptions")
    void testNullOrEmptyInput() {
        assertFalse(ProtectiveContextDetector.containsUnnegatedPaymentFeeDemand(null));
        assertFalse(ProtectiveContextDetector.containsUnnegatedPaymentFeeDemand(""));
        assertFalse(ProtectiveContextDetector.containsUnnegatedPaymentFeeDemand("   \n\t  "));
        assertFalse(ProtectiveContextDetector.containsUnnegatedCredentialPhishing(null));
        assertFalse(ProtectiveContextDetector.containsUnnegatedCredentialPhishing(""));
    }
}
