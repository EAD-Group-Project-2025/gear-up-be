package com.ead.gearup.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDTO {

    private Long id;
    private String vin;
    private String licensePlate;
    private Integer year;
    private String model;
    private String make;
}
