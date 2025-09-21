package com.ead.gearup.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ead.gearup.dto.request.ResendEmailRequestDTO;
import com.ead.gearup.dto.response.JwtTokensDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.exception.EmailAlreadyExistsException;
import com.ead.gearup.exception.InvalidRefreshTokenException;
import com.ead.gearup.exception.ResendEmailCooldownException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CustomUserDetailsService;
import com.ead.gearup.service.auth.JwtService;

import io.jsonwebtoken.ExpiredJwtException;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final EmailVerificationService emailVerificationService;

    private static final int COOLDOWN_MINUTES = 5;

    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {

        String email = userCreateDTO.getEmail().trim().toLowerCase();

        User user = User.builder()
                .email(email)
                .name(userCreateDTO.getName())
                .password(encoder.encode(userCreateDTO.getPassword()))
                .createdAt(LocalDateTime.now())
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);
        }

        emailVerificationService.sendVerificationEmail(user);

        // Return created user info (without password)
        return new UserResponseDTO(user.getEmail(), user.getName());
    }

    public boolean verifyEmailToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            String tokenType = jwtService.extractClaim(token, claims -> claims.get("token_type", String.class));

            if (!"email_verification".equals(tokenType)) {
                return false;
            }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (user.getIsVerified()) {
                return true; // already verified
            }

            user.setIsVerified(true);
            userRepository.save(user);

            return true;

        } catch (ExpiredJwtException e) {
            return false;
        } catch (UserNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void resendEmail(ResendEmailRequestDTO resendEmailRequestDTO) {

        String email = resendEmailRequestDTO.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getIsVerified()) {
            throw new IllegalStateException("User is already verified");
        }

        // Cooldown check
        if (user.getLastVerificationEmailSent() != null &&
                user.getLastVerificationEmailSent()
                        .isAfter(java.time.LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES))) {
            long waitMinutes = COOLDOWN_MINUTES - java.time.Duration.between(
                    user.getLastVerificationEmailSent(), java.time.LocalDateTime.now()).toMinutes();
            throw new ResendEmailCooldownException("Please wait " + waitMinutes + " minutes before requesting again");
        }

        emailVerificationService.sendVerificationEmail(user);

        // Update last sent timestamp
        user.setLastVerificationEmailSent(java.time.LocalDateTime.now());
        userRepository.save(user);

    }

    public JwtTokensDTO verifyUser(UserLoginDTO userLoginDTO) {
        Authentication authentication = authManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(userLoginDTO.getEmail(), userLoginDTO.getPassword()));

        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("invalid credintials!");
        }

        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        User user = userPrinciple.getUser();

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(userPrinciple);
        String refreshToken = jwtService.generateRefreshToken(userPrinciple);

        return new JwtTokensDTO(accessToken, refreshToken);
    }

    public LoginResponseDTO getRefreshAccessToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        if (!jwtService.validateRefreshToken(refreshToken, userDetails)) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setAccessToken(newAccessToken);

        return loginResponse;
    }
}
