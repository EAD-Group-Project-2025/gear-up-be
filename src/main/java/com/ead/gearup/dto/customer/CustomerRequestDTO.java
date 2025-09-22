package com.ead.gearup.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:0|\\+94)?7[0-9]{8}$", message = "Phone number must be a valid Sri Lankan number")
    private String phoneNumber;
}
