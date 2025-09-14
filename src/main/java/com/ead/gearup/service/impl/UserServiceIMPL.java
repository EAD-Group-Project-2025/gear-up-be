package com.ead.gearup.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.UserService;
import com.ead.gearup.service.auth.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceIMPL implements UserService {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    @Override
    public String createUser(UserCreateDTO userCreateDTO) {

        // Create User entity
        User user = User.builder()
                .email(userCreateDTO.getEmail())
                .name(userCreateDTO.getName())
                .password(encoder.encode(userCreateDTO.getPassword()))
                .build();

        userRepository.save(user);

        return "User created successfully!";
    }

    public String verifyUser(UserLoginDTO userLoginDTO) {
        Authentication authentication = authManager
                .authenticate(
                        new UsernamePasswordAuthenticationToken(userLoginDTO.getEmail(), userLoginDTO.getPassword()));

        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("invalid creadintials!");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return jwtService.generateAccessToken(userDetails);
    }
}
