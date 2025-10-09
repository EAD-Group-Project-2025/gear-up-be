package com.ead.gearup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordChangeResponse {
    private String message;
    private boolean requiresPasswordChange;
}
