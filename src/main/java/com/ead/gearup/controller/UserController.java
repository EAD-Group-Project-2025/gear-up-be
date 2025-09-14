package com.ead.gearup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.service.UserService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody UserCreateDTO userCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userCreateDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<String> getToken(@RequestBody UserLoginDTO userLoginDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.verifyUser(userLoginDTO));
    }
}
