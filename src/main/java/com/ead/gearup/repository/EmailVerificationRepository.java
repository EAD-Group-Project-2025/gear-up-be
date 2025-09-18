package com.ead.gearup.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ead.gearup.model.EmailVerification;
import com.ead.gearup.model.User;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByUserAndOtpAndIsUsedFalseAndExpiresAtAfter(
            User user, String otp, LocalDateTime currentTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification e WHERE e.user.userId = :userId")
    void deleteByUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :currentTime")
    void deleteExpiredOtps(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT e.user FROM EmailVerification e WHERE e.otp = :otp")
    Optional<User> findUserByOtp(@Param("otp") String otp);
}
