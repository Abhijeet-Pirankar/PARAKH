# PARAKH Prototype Development Dataset

> **IMPORTANT DISCLAIMER**:
> This dataset is a **synthetic, curated prototype/development dataset** created specifically for local development, baseline benchmarking, and continuous integration testing of the PARAKH Stage 10 AI/ML service.
> 
> **It is NOT production-grade training data.** Real-world deployment requires training on extensive, verified real-world corpus of legitimate and fraudulent employment solicitations, reviewed and audited by domain and security experts.

## Dataset Specification
- **Format**: JSON array of objects (`{"text": str, "label": "LEGITIMATE" | "SUSPICIOUS", "category": str}`)
- **Total Samples**: 80
- **Class Balance**: 40 `LEGITIMATE` (50%), 40 `SUSPICIOUS` (50%)
- **Categories Covered**:
  - `genuine_job_offer`: Authentic enterprise employment offers (software engineer, data analyst, product designer, cloud architect, etc.)
  - `genuine_internship`: Authentic college/freshers internships with formal interview stages and stipend details.
  - `genuine_recruiter_comm`: Professional correspondence inviting candidate to technical rounds or discussing onboarding.
  - `registration_fee_scam`: Solicitations demanding registration fees, refundable deposits, processing charges, or laptop shipping funds.
  - `training_fee_scam`: Mandatory upfront payments for training kits, onboarding materials, or placement certificates.
  - `fake_selection_scam`: Unconditional spot selection without technical evaluation, direct selection, urgent mass hiring.
  - `phishing_link_scam`: Links to third-party forms, suspicious TLDs, shortened URLs, credential theft portals.
  - `otp_password_phishing`: Direct requests for net banking passwords, OTP verification, or debit card CVVs.
  - `suspicious_recruiter_comm`: Demanding communication strictly via Telegram groups or WhatsApp for low-skill high-payout tasks.
