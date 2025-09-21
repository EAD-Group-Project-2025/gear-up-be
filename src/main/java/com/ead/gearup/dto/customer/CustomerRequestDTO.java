package com.ead.gearup.dto.customer;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequestDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
