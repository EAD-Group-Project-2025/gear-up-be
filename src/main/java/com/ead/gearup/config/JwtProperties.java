package com.ead.gearup.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JwtProperties {

    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    @Positive(message = "JWT expiration must be a positive number")
    private long expiration;

    @Positive(message = "JWT refresh expiration must be a positive number")
    private long refreshExpiration;
}
