package com.ead.gearup.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateDTO {

    @NotBlank(message = "VIN is required")
    @Size(min = 11, max = 17, message = "VIN must be between 11 and 17 characters")
    private String vin;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Make is required")
    private String make; // Vehicle manufacturer

}
