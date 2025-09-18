package com.ead.gearup.service;

import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ead.gearup.model.EmailVerification;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.EmailVerificationRepository;
import com.ead.gearup.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;

    @Value("${otp.expiration}")
    private int otpExpiration;

    @Value("${app.base-url}")
    private String appBaseUrl;

    public void sendVerificationEmail(User user) {
        try {
            // Delete any existing OTPs for this user
            emailVerificationRepository.deleteByUser(user.getUserId());

            // Generate new OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(999999));
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiration);

            EmailVerification verification = EmailVerification.builder()
                    .user(user)
                    .otp(otp)
                    .expiresAt(expiresAt)
                    .build();

            // Save OTP to database first
            emailVerificationRepository.save(verification);

            String confirmationUrl = appBaseUrl + "/api/auth/v1/verify-email?otp=" + otp;

            String subject = "Email Verification";
            String content = "Click the link to verify your email: " + confirmationUrl;

            // Send email
            emailService.sendVerificationEmail(user.getEmail(), subject, content);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    public boolean verifyOTP(String otp) {
        var verification = emailVerificationRepository.findByOtpAndIsUsedFalseAndExpiresAtAfter(otp,
                LocalDateTime.now());

        if (verification.isPresent()) {
            EmailVerification entity = verification.get();
            entity.setIsUsed(true);
            entity.setVerifiedAt(LocalDateTime.now());
            emailVerificationRepository.save(entity);

            User user = entity.getUser();
            user.setIsVerified(true);
            userRepository.save(user);

            return true;
        }

        return false;
    }

}
