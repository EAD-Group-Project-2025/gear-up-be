package com.ead.gearup.dto.vehicle;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateDTO {

    @Size(min = 11, max = 17, message = "VIN must be between 11 and 17 characters")
    private String vin;

    private String licensePlate;

    private Integer year;

    private String model;

    private String make; // Vehicle manufacturer

}
