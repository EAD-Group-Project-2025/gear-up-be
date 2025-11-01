package com.ead.gearup.dto.response;

import com.ead.gearup.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private String email;
    private String name;
    private UserRole role;
    private Boolean verified;
}
