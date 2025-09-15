package com.ead.gearup.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtTokensDTO {
    private String accessToken;
    private String refreshToken;
}
