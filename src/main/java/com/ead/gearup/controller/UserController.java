package com.ead.gearup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.auth.AuthRequest;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.service.JWTService;
import com.ead.gearup.service.UserService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    @PostMapping("/create-user")
    public ResponseEntity<UserCreateDTO> createUser(@RequestBody UserCreateDTO userCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userCreateDTO));
    }

    @PostMapping("/generate-token")
    public ResponseEntity<String> getToken(@RequestBody AuthRequest authRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        if (!authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Invalid user request!");
        }

        return ResponseEntity.status(HttpStatus.OK).body(jwtService.generateToken(authRequest.getUsername()));
    }
}
