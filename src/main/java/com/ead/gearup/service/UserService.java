package com.ead.gearup.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ead.gearup.dto.response.JwtTokensDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.exception.EmailAlreadyExistsException;
import com.ead.gearup.exception.InvalidRefreshTokenException;
import com.ead.gearup.exception.UsernameNotFoundException;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CustomUserDetailsService;
import com.ead.gearup.service.auth.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final EmailVerificationService emailVerificationService;

    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {

        String email = userCreateDTO.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .name(userCreateDTO.getName())
                .password(encoder.encode(userCreateDTO.getPassword()))
                .build();

        userRepository.save(user);

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
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (user.getIsVerified()) {
                return true; // already verified
            }

            user.setIsVerified(true);
            userRepository.save(user);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public JwtTokensDTO verifyUser(UserLoginDTO userLoginDTO) {
        Authentication authentication = authManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(userLoginDTO.getEmail(), userLoginDTO.getPassword()));

        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("invalid credintials!");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

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
